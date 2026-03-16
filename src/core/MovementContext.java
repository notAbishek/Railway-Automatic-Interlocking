package core;

import java.util.Map;

public final class MovementContext {

    private final String trainId;
    private final String trackId;
    private final Map<String, String> blockedBy;

    private String blockerTrainId;
    private boolean junctionLocked;
    private boolean overlapClear;
    private double requiredClearanceMetres;
    private double availableClearanceMetres;
    private boolean safeToProceed;
    private String denialReason;

    public MovementContext(String trainId, String trackId,
                           Map<String, String> blockedBy) {
        this.trainId = trainId;
        this.trackId = trackId;
        this.blockedBy = blockedBy;
        this.blockerTrainId = null;
        this.junctionLocked = false;
        this.overlapClear = false;
        this.requiredClearanceMetres = 0.0;
        this.availableClearanceMetres = 0.0;
        this.safeToProceed = false;
        this.denialReason = null;
    }

    public String getTrainId() { return trainId; }
    public String getTrackId() { return trackId; }
    public Map<String, String> getBlockedBy() { return blockedBy; }

    public String getBlockerTrainId() { return blockerTrainId; }
    public void setBlockerTrainId(String blockerTrainId) {
        this.blockerTrainId = blockerTrainId;
    }

    public boolean isJunctionLocked() { return junctionLocked; }
    public void setJunctionLocked(boolean junctionLocked) {
        this.junctionLocked = junctionLocked;
    }

    public boolean isOverlapClear() { return overlapClear; }
    public void setOverlapClear(boolean overlapClear) {
        this.overlapClear = overlapClear;
    }

    public double getRequiredClearanceMetres() {
        return requiredClearanceMetres;
    }

    public void setRequiredClearanceMetres(double requiredClearanceMetres) {
        this.requiredClearanceMetres = requiredClearanceMetres;
    }

    public double getAvailableClearanceMetres() {
        return availableClearanceMetres;
    }

    public void setAvailableClearanceMetres(double availableClearanceMetres) {
        this.availableClearanceMetres = availableClearanceMetres;
    }

    public boolean isSafeToProceed() { return safeToProceed; }
    public void setSafeToProceed(boolean safeToProceed) {
        this.safeToProceed = safeToProceed;
    }

    public String getDenialReason() { return denialReason; }
    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }
}