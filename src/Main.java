// Main.java
import core.*;
import enums.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import model.*;

public class Main {
    public static void main(String[] args) {

        DateTimeFormatter fmt = Dispatcher.FORMAT;

        Node origin1 = new OriginNode("ORIGIN_T1", "Origin of Train 1");
        Node signalNode1 = new SignalNode("1", "Signal 1", Facing.LEFT);
        Node signalNode2 = new SignalNode("2", "Signal 2", Facing.LEFT);
        Node signalNode3 = new SignalNode("3", "Signal 3", Facing.LEFT);
        Node signalNode4 = new SignalNode("4", "Signal 4", Facing.LEFT);

        Track track0   = new Track("T0", "Spawn Track", origin1, signalNode1, 100, 10);
        Track track1 = new Track("1", "Track 1", signalNode1, signalNode2, 250, 20);
        Track track2 = new Track("2", "Track 2", signalNode2, signalNode3, 300,30);
        Track track3 = new Track("3", "Track 3", signalNode3, signalNode4, 200,20);
        Track track4 = new Track("4", "Track 4", signalNode1, signalNode3, 400, 10);
        

        GraphBuilder graph = new GraphBuilder();

        graph.addTrack(track0);
        graph.addTrack(track1);
        graph.addTrack(track2);
        graph.addTrack(track3);
        graph.addTrack(track4);

        Train train1 = new Train("T1", "Train 1", TrainType.PASSENGER, TrainPriority.EXPRESS, track0.getId(), Direction.RIGHT, origin1.getId(), signalNode4.getId(), 0.0, LocalDateTime.parse("25-06-2026 08:00:00", fmt), LocalDateTime.parse("26-06-2026 08:00:00", fmt));
        Train train2 = new Train("T2", "Train 2", TrainType.ENGINE, TrainPriority.EXPRESS, track1.getId(), Direction.RIGHT, signalNode1.getId(), signalNode3.getId(), 0.0, LocalDateTime.parse("25-06-2026 08:00:00", fmt), LocalDateTime.parse("26-06-2026 08:00:00", fmt));

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.addTrain(train1);
        dispatcher.addTrain(train2);

        System.out.println("--- Dispatch Order ---");
        while (!dispatcher.isEmpty()) {
            Train t = dispatcher.dispatch();
            System.out.println(t.getId()
                + " | " + t.getPriority()
                + " | " + t.getDepartureTime().format(fmt));
        }
    }
}
