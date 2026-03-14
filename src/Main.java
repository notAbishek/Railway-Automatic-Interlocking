import core.*;
import enums.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import model.*;
import scheduler.*;

public class Main {
    public static void main(String[] args) {

        DateTimeFormatter fmt = Dispatcher.FORMAT;

        // Nodes
        Node origin1     = new OriginNode("ORIGIN_T1", "Origin of Train 1");
        Node origin2     = new OriginNode("ORIGIN_T2", "Origin of Train 2");
        Node signalNode1 = new SignalNode("1", "Signal 1", Facing.LEFT);
        Node signalNode2 = new SignalNode("2", "Signal 2", Facing.LEFT);
        Node signalNode3 = new SignalNode("3", "Signal 3", Facing.LEFT);
        Node signalNode4 = new SignalNode("4", "Signal 4", Facing.LEFT);

        // Tracks — distance, minSpeedLimit, maxSpeedLimit
        Track track0 = new Track("T0", "Track 0", origin1,     signalNode1, 100,   5,  20);
        Track track1 = new Track("1",  "Track 1",  signalNode1, signalNode2, 250, 5, 30);
        Track track2 = new Track("2",  "Track 2",  signalNode2, signalNode3, 300, 5, 30);
        Track track3 = new Track("3",  "Track 3",  signalNode3, signalNode4, 200, 5, 25);

        // Graph
        GraphBuilder graph = new GraphBuilder();
        graph.addTrack(track0);
        graph.addTrack(track1);
        graph.addTrack(track2);
        graph.addTrack(track3);

        // Trains — speed 0.0 (at station), departure time
        Train train1 = new Train("T1", "Train 1",
            TrainType.PASSENGER, TrainPriority.EXPRESS, track0.getId(),
            Direction.RIGHT, origin1.getId(), signalNode4.getId(),
            0.0,
            LocalDateTime.parse("25-06-2026 08:00:00", fmt),
            LocalDateTime.parse("25-06-2026 08:02:00", fmt));  // planned arrival


        List<Train> trains = new ArrayList<>();
        trains.add(train1);

        // Run IntervalBuilder
        IntervalBuilder intervalBuilder = new IntervalBuilder(graph);

        Map<String, List<Track>>        paths     = intervalBuilder.buildPaths(trains);
        Map<String, List<TrackInterval>> intervals = intervalBuilder.buildIntervals(trains, paths);

        intervalBuilder.printIntervals(intervals);
    }
}
