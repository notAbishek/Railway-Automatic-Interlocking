import core.*;
import enums.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import model.*;
import scheduler.*;
import signal.*;

public class Main {
    public static void main(String[] args) {

        DateTimeFormatter fmt = Dispatcher.FORMAT;

        // Nodes
        Node origin1     = new OriginNode("ORIGIN_T1", "Origin of Train 1");
        Node origin2     = new OriginNode("ORIGIN_T2", "Origin of Train 2");
        Node signalNode1 = new SignalNode("1", "Signal 1");
        Node signalNode2 = new SignalNode("2", "Signal 2");
        Node signalNode3 = new SignalNode("3", "Signal 3");
        Node signalNode4 = new SignalNode("4", "Signal 4");

        // Tracks — distance, minSpeedLimit, maxSpeedLimit
        Track track0  = new Track("T0", "Track 0", origin1,     signalNode1, 100,   5,  20);
        Track track0b = new Track("T0B","Track 0B",origin2,     signalNode4, 100,   5,  20);
        Track track1  = new Track("1",  "Track 1",  signalNode1, signalNode2, 250, 5, 30);
        Track track2  = new Track("2",  "Track 2",  signalNode2, signalNode3, 300, 5, 30);
        Track track3  = new Track("3",  "Track 3",  signalNode3, signalNode4, 200, 5, 25);

        // Graph
        GraphBuilder graph = new GraphBuilder();
        graph.addTrack(track0);
        graph.addTrack(track0b);
        graph.addTrack(track1);
        graph.addTrack(track2);
        graph.addTrack(track3);

        LocalDateTime dep1;
        LocalDateTime arr1;
        LocalDateTime dep2;
        LocalDateTime arr2;

        try {
            dep1 = LocalDateTime.parse("25-06-2026 08:00:00", fmt);
            arr1 = LocalDateTime.parse("25-06-2026 08:02:00", fmt);
            dep2 = LocalDateTime.parse("25-06-2026 08:00:30", fmt);
            arr2 = LocalDateTime.parse("25-06-2026 08:03:00", fmt);
        } catch (java.time.format.DateTimeParseException e) {
            System.err.println("Invalid date format: " + e.getMessage());
            return;
        }

        // Trains
        Train train1 = new Train("T1", "Train 1",
            TrainType.PASSENGER, TrainPriority.EXPRESS, track0.getId(),
            origin1.getId(), signalNode4.getId(),
            0.0,
            dep1,
            arr1);

        Train train2 = new Train("T2", "Train 2",
            TrainType.PASSENGER, TrainPriority.LOCAL, track0b.getId(),
            origin2.getId(), signalNode1.getId(),
            0.0,
            dep2,
            arr2);

        // --- SCHEDULER (pre-dispatch) ---
        List<Train> trains = new ArrayList<>();
        trains.add(train1);
        trains.add(train2);

        TrainScheduler scheduler = new TrainScheduler(graph);
        TrainScheduler.SchedulerResult result = scheduler.schedule(trains);

        System.out.println("\n--- DISPATCH ORDER ---");
        for (Train t : result.getOrderedTrains()) {
            System.out.println(t.getId()
                + " | " + t.getPriority()
                + " | departs: " + t.getDepartureTime().format(fmt)
                + " | planned arrival: " + t.getArrivalTime().format(fmt));
            if (t.getDelayHours() > 0) {
                System.out.println("  DELAY: "
                    + String.format("%.1f", t.getDelayHours()) + " hrs");
            }
        }

        // Print intervals
        IntervalBuilder ib = new IntervalBuilder(graph);
        ib.printIntervals(result.getIntervals());

        // --- DISPATCHER (dispatch loop) ---
        ConflictDetector  conflictDetector  = new ConflictDetector();
        SignalController  signalController  = new SignalController();

        Dispatcher dispatcher = new Dispatcher();
        for (Train t : result.getOrderedTrains()) {
            dispatcher.addTrain(t);
        }

        System.out.println("\n--- SIMULATION ---");
        while (!dispatcher.isEmpty()) {
            Train train = dispatcher.dispatch();
            List<TrackTraversal> path =
                result.getPaths().get(train.getId());

            if (path == null || path.isEmpty()) {
                System.err.println("No path found for dispatched train "
                    + train.getId() + "; skipping dispatch cycle.");
                continue;
            }

            System.out.println("\nDispatching: " + train.getId());

            for (int i = 0; i < path.size(); i++) {
                TrackTraversal traversal = path.get(i);
                Track track     = traversal.getTrack();
                Track nextTrack = (i + 1 < path.size())
                    ? path.get(i + 1).getTrack() : null;

                // Find junction if track's endNode is a junction
                JunctionNode junction = null;
                if (track.getEndNode() instanceof JunctionNode) {
                    junction = (JunctionNode) track.getEndNode();
                }

                // Find the signal protecting this track
                SignalNode signal = null;
                if (track.getStartNode() instanceof SignalNode) {
                    signal = (SignalNode) track.getStartNode();
                }

                MovementContext context = conflictDetector.assess(
                    track, nextTrack, traversal, junction, train);
                boolean safe = context.isSafeToProceed();

                if (safe) {
                    boolean cleared = true;
                    if (signal != null) {
                        cleared = signalController.requestGreen(
                            signal, track, nextTrack, traversal,
                            junction, context, graph);
                    }

                    if (!cleared) {
                        if (signal != null) {
                            signalController.setRed(signal, graph);
                        }
                        System.out.println("  HELD at " + track.getId()
                            + " — signal did not clear");
                        dispatcher.addTrain(train);
                        break;
                    }

                    conflictDetector.onTrackEntry(
                        train, track, traversal, junction);
                    if (signal != null) {
                        signalController.validateSpeed(train, track,
                            signal, graph);
                    }
                    System.out.println("  ENTER: " + track.getId()
                        + " [" + traversal.getDirection() + "]");
                    conflictDetector.onTrackExit(train, track, junction);
                    System.out.println("  EXIT:  " + track.getId());
                    if (signal != null) {
                        signalController.setRed(signal, graph);
                    }
                } else {
                    if (signal != null) {
                        signalController.setRed(signal, graph);
                    }
                    System.out.println("  HELD at " + track.getId()
                        + " — conflict detected");
                    // Requeue train for retry
                    dispatcher.addTrain(train);
                    break;
                }
            }

            System.out.println("  ARRIVED: " + train.getEndNode());
        }
    }
}
