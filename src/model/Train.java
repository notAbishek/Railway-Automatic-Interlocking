package model;

import enums.Direction;
import enums.TrainType;

public class Train {
    final private String id;
    private String name;
    private TrainType type;
    private TrainPriority priority;
    private String trackOnUse;
    private Direction direction;
    private String startNode;
    private String endNode;
    private double speed = 0;

    public Train(
        String id,
        String name,
        TrainType type,
        TrainPriority priority,
        Direction direction,
        String startNode,
        String endNode
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.priority = priority;
        this.direction = direction;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public TrainType getType() {
        return this.type;
    }

    public TrainPriority getPriority() {
        return this.priority;
    }

    public String getTrackOnUse() {
        return this.trackOnUse;
    }
    public Direction getDirection() {
        return this.direction;
    }

    public String getStartNode() {
        return this.startNode;
    }

    public String getEndNode() {
        return this.endNode;
    }

    public double getSpeed() {
        return this.speed;
    }




}
