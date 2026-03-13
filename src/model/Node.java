package model;

public abstract class Node {
    final protected String id;
    final protected String name;

    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
