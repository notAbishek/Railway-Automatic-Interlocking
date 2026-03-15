package scheduler;

import conflict.TimeWindowConflict;
import java.time.Duration;
import java.util.*;
import model.TrackInterval;
import model.Train;

public class MeetAndPassResolver {

    private final TimeWindowConflict timeWindowConflict
        = new TimeWindowConflict();

    // Modifies train departureTime in-place for conflicting trains.
    // Also updates their TrackIntervals to reflect new times.
    public void resolve(
            List<Train> trains,
            Map<String, List<TrackInterval>> intervals) {

        Map<String, Train> trainMap = new HashMap<>();
        for (Train t : trains) {
            trainMap.put(t.getId(), t);
        }

        // Max 10 passes to prevent infinite loop
        for (int pass = 0; pass < 10; pass++) {
            boolean anyConflict = false;

            List<String> trainIds = new ArrayList<>(intervals.keySet());

            for (int a = 0; a < trainIds.size(); a++) {
                for (int b = a + 1; b < trainIds.size(); b++) {
                    String idA = trainIds.get(a);
                    String idB = trainIds.get(b);

                    List<TrackInterval> intA = intervals.get(idA);
                    List<TrackInterval> intB = intervals.get(idB);
                    Train trainA = trainMap.get(idA);
                    Train trainB = trainMap.get(idB);

                    if (trainA == null || trainB == null) continue;

                    for (TrackInterval iA : intA) {
                        for (TrackInterval iB : intB) {
                            if (timeWindowConflict.hasConflict(iA, iB)) {
                                anyConflict = true;

                                // Determine which train has lower priority
                                Train higher, lower;
                                TrackInterval hiInt, loInt;
                                String lowerId;

                                if (trainA.getPriority().ordinal()
                                        < trainB.getPriority().ordinal()) {
                                    higher = trainA; lower = trainB;
                                    hiInt = iA; loInt = iB;
                                } else if (trainA.getPriority().ordinal()
                                        > trainB.getPriority().ordinal()) {
                                    higher = trainB; lower = trainA;
                                    hiInt = iB; loInt = iA;
                                } else {
                                    // Same priority: later-departing yields
                                    if (trainA.getDepartureTime()
                                            .isAfter(trainB.getDepartureTime())) {
                                        higher = trainB; lower = trainA;
                                        hiInt = iB; loInt = iA;
                                    } else {
                                        higher = trainA; lower = trainB;
                                        hiInt = iA; loInt = iB;
                                    }
                                }

                                lowerId = lower.getId();
                                long delay = timeWindowConflict
                                    .getRequiredDelaySeconds(hiInt, loInt);

                                if (delay > 0) {
                                    lower.setDepartureTime(
                                        lower.getDepartureTime()
                                             .plusSeconds(delay));
                                    Duration dur = Duration.ofSeconds(delay);
                                    for (TrackInterval ti
                                            : intervals.get(lowerId)) {
                                        ti.delay(dur);
                                    }
                                    System.out.println("MEET-AND-PASS: "
                                        + lowerId + " delayed " + delay
                                        + "s on " + iA.getTrackId());
                                }
                            }
                        }
                    }
                }
            }

            if (!anyConflict) break;
        }
    }
}
