package model;

public class SignalNode extends Node {
    private SignalState state = SignalState.RED;

    public SignalNode() {
    }

    public SignalNode(String id, String name) {
        super(id, name);
    }
}
