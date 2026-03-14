package scheduler;

import core.GraphBuilder;
import core.PathFinder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Track;
import model.TrackInterval;
import model.Train;

public class IntervalBuilder {

    private final GraphBuilder graph;

    public IntervalBuilder(GraphBuilder graph) {
        this.graph = graph;
    }

    // ─────────────────────────────────────────────
    // STEP 1 — Run PathFinder for every train upfront
    // Returns map of trainId → ordered List<Track>
    // ─────────────────────────────────────────────
    public Map<String, List<Track>> buildPaths(List<Train> trains) {
        Map<String, List<Track>> paths = new HashMap<>();

        for (Train train : trains) {
            PathFinder  pathFinder = new PathFinder(train, graph);
            List<Track> path       = pathFinder.findPath();

            if (path.isEmpty()) {
                throw new IllegalStateException(
                    "No path found for train " + train.getId()
                    + " | from: " + train.getStartNode()
                    + " | to: "   + train.getEndNode()
                    + " | direction: " + train.getDirection()
                );
            }

            paths.put(train.getId(), path);

            System.out.println("PATH [" + train.getId() + "]: "
                + pathToString(path));
        }

        return paths;
    }

    // ─────────────────────────────────────────────
    // STEP 2 — Build TrackIntervals for every train
    // Returns map of trainId → List<TrackInterval>
    // Also sets actualArrivalTime and delayHours on each Train
    // ─────────────────────────────────────────────
    public Map<String, List<TrackInterval>> buildIntervals(
            List<Train> trains,
            Map<String, List<Track>> paths) {

        Map<String, List<TrackInterval>> allIntervals = new HashMap<>();

        for (Train train : trains) {
            List<Track>         path      = paths.get(train.getId());
            List<TrackInterval> intervals = computeIntervals(train, path);

            allIntervals.put(train.getId(), intervals);

            // Set actual arrival on train from last interval's exit time
            if (!intervals.isEmpty()) {
                LocalDateTime actualArrival = intervals
                    .get(intervals.size() - 1)
                    .getExitTime();

                train.setActualArrivalTime(actualArrival);

                // Log delay if any
                if (train.getDelayHours() > 0) {
                    System.out.println("DELAY  [" + train.getId() + "]: "
                        + String.format("%.1f", train.getDelayHours())
                        + " hours late"
                        + " | planned: "  + train.getArrivalTime()
                        + " | actual: "   + actualArrival);
                } else {
                    System.out.println("ON TIME [" + train.getId() + "]: "
                        + "arrives " + actualArrival);
                }
            }
        }

        return allIntervals;
    }

    // ─────────────────────────────────────────────
    // Compute intervals for one train across its full path
    // enterTime and exitTime are LocalDateTime — no epoch conversion
    // ─────────────────────────────────────────────
    private List<TrackInterval> computeIntervals(Train train, List<Track> path) {

        List<TrackInterval> intervals   = new ArrayList<>();
        LocalDateTime       currentTime = train.getDepartureTime();

        for (Track track : path) {
            double speedLimit = track.getMinSpeedLimit();
            int    distance   = track.getDistance();

            // Spawn track (distance=0) or no speed limit — no travel time, always takes 1 second extra to avoid zero-length intervals
            long travelSeconds = (speedLimit > 0 && distance > 0)
                ? (long) Math.ceil(distance / speedLimit)
                : 1;

            LocalDateTime enterTime = currentTime;
            LocalDateTime exitTime  = currentTime.plusSeconds(travelSeconds);

            intervals.add(new TrackInterval(train.getId(), track.getId(), enterTime, exitTime, train.getDirection()));

            currentTime = exitTime;
        }

        return intervals;
    }

    // ─────────────────────────────────────────────
    // UTILITY — print all intervals for debugging
    // ─────────────────────────────────────────────
    public void printIntervals(
            Map<String, List<TrackInterval>> allIntervals) {

        System.out.println("\n=== TRACK INTERVALS ===");
        for (Map.Entry<String, List<TrackInterval>> entry
                : allIntervals.entrySet()) {

            System.out.println("Train " + entry.getKey() + ":");
            for (TrackInterval interval : entry.getValue()) {
                Duration occupancy = interval.getOccupancyDuration();
                System.out.println("  " + interval
                    + " (" + occupancy.toSeconds() + "s)");
            }
        }
        System.out.println("=======================\n");
    }

    // Format path as readable string for logging
    private String pathToString(List<Track> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Track t = path.get(i);
            sb.append(t.getStartNode().getId())
              .append(" -[")
              .append(t.getId())
              .append("]-> ")
              .append(t.getEndNode().getId());
            if (i < path.size() - 1) sb.append(" | ");
        }
        return sb.toString();
    }
}