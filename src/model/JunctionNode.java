package model;

import enums.JunctionDirection;

public class JunctionNode extends Node {
    private String primaryNode;
    private String secondaryNode;
    private boolean state = false;
    private JunctionDirection direction;

    public JunctionNode(
        String id,
        String name,
        String primaryNode,
        String secondaryNode,
        boolean state,
        JunctionDirection direction
    ) {
        super(id, name);
        this.primaryNode = primaryNode;
        this.secondaryNode = secondaryNode;
        this.state = state;
        this.direction = direction;
    }
}
