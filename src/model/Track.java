package model;

import enums.Direction;
import java.util.ArrayList;
import java.util.List;

public class Track {
    private String id;
    private String name;
    private Node startNode;
    private Node endNode;
    private int distance;
    private boolean inUse;
    private List<String> usedBy;
    private Direction occupiedDirection;
    private double trackSpeedLimit;

    public Track() {
        this.inUse = false;
        this.usedBy = new ArrayList<>();
        this.trackSpeedLimit = 5.0;
    }

    public Track(String id, String name, Node startNode, Node endNode, int distance, double trackSpeedLimit) {
        this.id = id;
        this.name = name;
        this.startNode = startNode;
        this.endNode = endNode;
        this.distance = distance;
        this.trackSpeedLimit = trackSpeedLimit;
        this.occupiedDirection = null;
        this.inUse = false;
        this.trackSpeedLimit = trackSpeedLimit;
        this.usedBy = new ArrayList<>();
    }
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Node getStartNode() {
        return this.startNode;
    }

    public Node getEndNode() {
        return this.endNode;
    }

    public int getDistance() {
        return this.distance;
    }

    public boolean isInUse() {
        return this.inUse;
    }

    public List<String> getUsedBy() {
        return this.usedBy;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public void addUsedBy(String trainId) {
        this.usedBy.add(trainId);
    }

    public Direction getOccupiedDirection() {
        return this.occupiedDirection;
    }

}
