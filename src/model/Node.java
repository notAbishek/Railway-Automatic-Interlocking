package model;

public abstract class Node {
    private final String id;
    private final String name;
    protected double x;
    protected double y;

    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Node(String id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public abstract String getType();

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }
}
