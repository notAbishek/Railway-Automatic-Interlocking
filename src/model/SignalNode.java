package model;

public class SignalNode extends Node {
    private SignalState state;

    public SignalNode() {
    }

    public SignalNode(String id, String name, SignalState state) {
        super(id, name);
        this.state = state;
    }
}
