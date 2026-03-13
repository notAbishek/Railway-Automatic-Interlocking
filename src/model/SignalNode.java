package model;

import enums.Facing;

public class SignalNode extends Node {
    private SignalState state = SignalState.RED;
    final private Facing facing;


    public SignalNode(String id, String name, Facing facing) {
        super(id, name);
        this.facing = facing;
    }
}
