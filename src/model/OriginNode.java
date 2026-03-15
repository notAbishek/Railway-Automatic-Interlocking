package model;

public class OriginNode extends Node {

    public OriginNode(String id, String name) {
        super(id, name);
    }

    @Override
    public final String getType() {
        return "ORIGIN";
    }
}