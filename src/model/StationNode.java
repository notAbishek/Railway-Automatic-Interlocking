package model;

public class StationNode extends Node {
    private String stationCode;
    private int platformCount;


    public StationNode(String id, String name, String stationCode, int platformCount) {
        super(id, name);
        this.stationCode = stationCode;
        this.platformCount = platformCount;
    }
}
