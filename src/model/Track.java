package model;

import enums.Direction;
import enums.TrainType;
import java.time.LocalDateTime;
import java.util.List;

public class Track {

    public enum TrackGeometry {
        STRAIGHT,
        CURVE
    }

    private final String id;
    private final String name;
    private final Node startNode;
    private final Node endNode;
    private final int distance;
    private Reservation activeReservation;
    private final double minSpeedLimit;
    private final double maxSpeedLimit;
    private List<TrainType> allowedTypes;
    private TrackGeometry geometry;      // STRAIGHT or CURVE, for visualization only
    private double        curveAngle;    // degrees (0-360), only used if CURVE
    private double  overlapMetres       = 180.0;
    // IR MACL rule: 180m ahead of signal must be clear before GREEN
    private Double  temporarySpeedLimit = null;
    // null = no TSR. Set when engineering work or track defect exists.
    private LocalDateTime tsrValidUntil = null;
    // TSR expires at this time. Null if no TSR.

    public Track(String id, String name, Node startNode, Node endNode, int distance, double minSpeedLimit, double maxSpeedLimit) {
        this.id = id;
        this.name = name;
        this.startNode = startNode;
        this.endNode = endNode;
        this.distance = distance;
        this.minSpeedLimit = minSpeedLimit;
        this.maxSpeedLimit = maxSpeedLimit;
        this.allowedTypes = null;
        this.activeReservation = null;
        this.geometry    = TrackGeometry.STRAIGHT;
        this.curveAngle  = 0.0;
    }
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public double getMinSpeedLimit() {
        return this.minSpeedLimit;
    }

    public double getMaxSpeedLimit() {
        return this.maxSpeedLimit;
    }

    public TrackGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(TrackGeometry geometry) {
        this.geometry = geometry;
    }

    public double getCurveAngle() {
        return curveAngle;
    }

    public void setCurveAngle(double curveAngle) {
        this.curveAngle = curveAngle;
    }

    // Convenience — true if this track is curved
    public boolean isCurve() {
        return geometry == TrackGeometry.CURVE;
    }

    // null or empty = all train types allowed on this track
    public boolean isAllowedFor(TrainType type) {
        if (allowedTypes == null || allowedTypes.isEmpty()) return true;
        return allowedTypes.contains(type);
    }

    public List<TrainType> getAllowedTypes() {
        return allowedTypes;
    }

    // Called when defining restricted tracks — e.g. freight-only
    public void setAllowedTypes(List<TrainType> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public Node getStartNode() {
        return this.startNode;
    }

    public Node getEndNode() {
        return this.endNode;
    }

    public int getDistance() {
        return this.distance;
    }

    public boolean isInUse() {
        return this.activeReservation != null;
    }

    public Reservation getActiveReservation() {
        return this.activeReservation;
    }

    public void reserve(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation cannot be null");
        }
        if (this.activeReservation != null) {
            throw new IllegalStateException(
                "Track " + this.id + " already reserved by "
                + this.activeReservation.trainId());
        }
        this.activeReservation = reservation;
    }

    public void reserve(String trainId, Direction direction,
                         long enterTime, long expectedExitTime) {
        reserve(new Reservation(trainId, direction, enterTime,
            expectedExitTime));
    }

    public void release(String trainId) {
        if (this.activeReservation == null) {
            return;
        }
        if (this.activeReservation.trainId().equals(trainId)) {
            this.activeReservation = null;
        }
    }

    public double getOverlapMetres() { return overlapMetres; }
    public void setOverlapMetres(double m) { this.overlapMetres = m; }

    public void setTemporarySpeedRestriction(double speed,
                                              LocalDateTime until) {
        this.temporarySpeedLimit = speed;
        this.tsrValidUntil       = until;
    }

    public void clearTemporarySpeedRestriction() {
        this.temporarySpeedLimit = null;
        this.tsrValidUntil       = null;
    }

    public double getEffectiveMaxSpeed() {
        if (temporarySpeedLimit != null && tsrValidUntil != null
         && LocalDateTime.now().isBefore(tsrValidUntil)) {
            return Math.min(maxSpeedLimit, temporarySpeedLimit);
        }
        return maxSpeedLimit;
    }

    public boolean hasTSR() {
        return temporarySpeedLimit != null
            && tsrValidUntil != null
            && LocalDateTime.now().isBefore(tsrValidUntil);
    }

}
