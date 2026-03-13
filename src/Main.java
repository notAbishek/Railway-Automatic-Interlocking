import enums.*;
import model.*;


public class Main {
    public static void main(String[] args) {
        SignalNode signalNode1 = new SignalNode("1", "Signal 1", Facing.LEFT);
        SignalNode signalNode2 = new SignalNode("2", "Signal 2", Facing.LEFT);
        SignalNode signalNode3 = new SignalNode("3", "Signal 3", Facing.LEFT);
        SignalNode signalNode4 = new SignalNode("4", "Signal 4", Facing.LEFT);

        Track track1 = new Track("1", "Track 1", signalNode1, signalNode2, 250);
        Track track2 = new Track("2", "Track 2", signalNode2, signalNode3, 300);
        Track track3 = new Track("3", "Track 3", signalNode3, signalNode4, 200);
        Track track4 = new Track("4", "Track 4", signalNode1, signalNode3, 400);

        Train train1 = new Train("T1", "Train 1", TrainType.PASSENGER, TrainPriority.EXPRESS, Direction.RIGHT, signalNode1.getName(), signalNode2.getName());
        //System.out.println(train1.getId());
        

    }
}
