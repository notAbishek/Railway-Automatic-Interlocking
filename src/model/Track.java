package model;

import enums.Direction;
import enums.TrainType;
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
    private double minSpeedLimit;
    private double maxSpeedLimit;
    private List<TrainType> allowedTypes;

    public Track() {
        this.inUse = false;
        this.usedBy = new ArrayList<>();
        this.minSpeedLimit = 0.0;
        this.maxSpeedLimit = 5.0;
        this.allowedTypes = null;
    }

    public Track(String id, String name, Node startNode, Node endNode, int distance, double minSpeedLimit, double maxSpeedLimit) {
        this.id = id;
        this.name = name;
        this.startNode = startNode;
        this.endNode = endNode;
        this.distance = distance;
        this.minSpeedLimit = minSpeedLimit;
        this.maxSpeedLimit = maxSpeedLimit;
        this.allowedTypes = null;
        this.occupiedDirection = null;
        this.inUse = false;
        this.usedBy = new ArrayList<>();
    }
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public double getMinSpeedLimit() {
        return this.minSpeedLimit;
    }

    public double getMaxSpeedLimit() {
        return this.maxSpeedLimit;
    }

    // null or empty = all train types allowed on this track
    public boolean isAllowedFor(TrainType type) {
        if (allowedTypes == null || allowedTypes.isEmpty()) return true;
        return allowedTypes.contains(type);
    }

    public List<TrainType> getAllowedTypes() {
        return allowedTypes;
    }

    // Called when defining restricted tracks — e.g. freight-only
    public void setAllowedTypes(List<TrainType> allowedTypes) {
        this.allowedTypes = allowedTypes;
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
