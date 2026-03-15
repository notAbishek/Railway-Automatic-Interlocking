package model;

import enums.JunctionDirection;

public class JunctionNode extends Node {
    private final String primaryNode;
    private final String secondaryNode;
    private boolean state = false;
    private final JunctionDirection direction;
    private boolean isolated         = false;
    // true = points are physically locked, cannot change state
    private String  isolatedByTrainId = null;
    // which train caused the isolation lock
    private double  foulingDistanceMetres = 0.0;
    // distance behind the junction that must be clear

    public JunctionNode(
            String id,
            String name,
            String primaryNode,
            String secondaryNode,
            boolean state,
            JunctionDirection direction) {
        super(id, name);
        this.primaryNode = primaryNode;
        this.secondaryNode = secondaryNode;
        this.state = state;
        this.direction = direction;
    }

    public JunctionNode(
            String id,
            String name,
            JunctionDirection direction,
            String primaryNode,
            String secondaryNode) {
        this(id, name, primaryNode, secondaryNode, false, direction);
    }

    @Override
    public final String getType() {
        return "JUNCTION";
    }

    public String getPrimaryNode() {
        return primaryNode;
    }

    public String getSecondaryNode() {
        return secondaryNode;
    }

    public boolean getState() {
        return state;
    }

    public JunctionDirection getDirection() {
        return direction;
    }

    public void isolate(String trainId) {
        if (trainId == null || trainId.isEmpty()) {
            throw new IllegalArgumentException("trainId cannot be null");
        }
        this.isolated          = true;
        this.isolatedByTrainId = trainId;
    }

    public void release(String trainId) {
        if (trainId.equals(this.isolatedByTrainId)) {
            this.isolated          = false;
            this.isolatedByTrainId = null;
        }
    }

    public boolean isIsolated()            { return isolated; }
    public String  getIsolatedByTrainId()  { return isolatedByTrainId; }
    public double  getFoulingDistanceMetres() { return foulingDistanceMetres; }
    public void    setFoulingDistanceMetres(double d) {
        this.foulingDistanceMetres = d;
    }

}
