// Dispatcher.java
package core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.PriorityQueue;
import model.Train;

public class Dispatcher {

    public static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final PriorityQueue<Train> trainQueue;

    public Dispatcher() {
        this.trainQueue = new PriorityQueue<>(
            (a, b) -> {
                // 1. Departure time — earliest first
                int timeComp = a.getDepartureTime().compareTo(b.getDepartureTime());
                if (timeComp != 0) return timeComp;

                // 2. TrainPriority — EXPRESS before LOCAL
                int priorityComp = a.getPriority().ordinal() - b.getPriority().ordinal();
                if (priorityComp != 0) return priorityComp;

                // 3. TrainType — ordinal order
                int typeComp = a.getType().ordinal() - b.getType().ordinal();
                if (typeComp != 0) return typeComp;

                // 4. Train ID — alphabetical, last tiebreaker
                return a.getId().compareTo(b.getId());
            }
        );
    }

    public void addTrain(Train train) {
        if (train == null) {
            throw new IllegalArgumentException("Cannot add null train");
        }

        LocalDateTime now = LocalDateTime.now();

        // If departure time is in the past — replace with current time
        if (train.getDepartureTime().isBefore(now)) {
            System.out.println("WARNING: Train " + train.getId()
                + " had past departure time ["
                + train.getDepartureTime().format(FORMAT)
                + "]. Replaced with current time ["
                + now.format(FORMAT) + "].");
            train.setDepartureTime(now);
        }

        trainQueue.offer(train);
    }

    public Train dispatch() {
        return trainQueue.poll();
    }

    public boolean isEmpty() {
        return trainQueue.isEmpty();
    }

    public int size() {
        return trainQueue.size();
    }
}