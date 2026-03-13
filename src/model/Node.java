package model;

public abstract class Node {
    protected String id;
    protected String name;

    public Node() {
    }

    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
