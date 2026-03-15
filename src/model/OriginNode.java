package model;

public class OriginNode extends Node {

    public OriginNode(String id, String name) {
        super(id, name);
    }

    @Override
    public String getType() {
        return "ORIGIN";
    }
}