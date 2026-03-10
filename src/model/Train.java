package model;

public class Train {
    private String id;
    private String name;
    private TrainType type;
    private TrainPriority priority;
    private String trackOnUse;
    private Direction direction;
    private String startNode;
    private String endNode;
    private double speed;

    public Train() {
    }

    public Train(
        String id,
        String name,
        TrainType type,
        TrainPriority priority,
        String trackOnUse,
        Direction direction,
        String startNode,
        String endNode,
        double speed
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.priority = priority;
        this.trackOnUse = trackOnUse;
        this.direction = direction;
        this.startNode = startNode;
        this.endNode = endNode;
        this.speed = speed;
    }
}
