package model;

public class JunctionNode extends Node {
    private String facingPointer;
    private String divergingPointer;
    private boolean state = false;
    private boolean isolated         = false;
    // true = points are physically locked, cannot change state
    private String  isolatedByTrainId = null;
    // which train caused the isolation lock
    private double  foulingDistanceMetres = 0.0;
    // distance behind the junction that must be clear

    public JunctionNode(
            String id,
            String name,
            String facingPointer,
            String divergingPointer,
            boolean state) {
        super(id, name);
        this.facingPointer = facingPointer;
        this.divergingPointer = divergingPointer;
        this.state = state;
    }

    public JunctionNode(
            String id,
            String name,
            String facingPointer,
            String divergingPointer) {
        this(id, name, facingPointer, divergingPointer, false);
    }

    @Override
    public final String getType() {
        return "JUNCTION";
    }

    public String getFacingPointer() {
        return facingPointer;
    }

    public void setFacingPointer(String facingPointer) {
        this.facingPointer = facingPointer;
    }

    public String getDivergingPointer() {
        return divergingPointer;
    }

    public void setDivergingPointer(String divergingPointer) {
        this.divergingPointer = divergingPointer;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        if (this.isolated) {
            throw new IllegalStateException(
                "Cannot change junction state while isolated by train "
                + this.isolatedByTrainId);
        }
        this.state = state;
    }

    public void lockForRoute(String trainId) {
        isolate(trainId);
    }

    public void releaseAfterClearance(String trainId,
                                       double clearedDistanceMetres) {
        if (clearedDistanceMetres >= this.foulingDistanceMetres) {
            release(trainId);
        }
    }

    public void isolate(String trainId) {
        if (trainId == null || trainId.isEmpty()) {
            throw new IllegalArgumentException("trainId cannot be null");
        }
        this.isolated          = true;
        this.isolatedByTrainId = trainId;
    }

    public void release(String trainId) {
        if (trainId == null) {
            return;
        }
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
