package core;

import conflict.DeadlockDetector;
import conflict.FollowingConflict;
import conflict.HeadOnConflict;
import java.util.*;
import model.*;

public class ConflictDetector {

    private final HeadOnConflict      headOn    = new HeadOnConflict();
    private final FollowingConflict   following = new FollowingConflict();
    private final DeadlockDetector    deadlock  = new DeadlockDetector();
    private final Map<String, String> blockedBy = new HashMap<>();

    public MovementContext assess(
            Track          track,
            Track          nextTrack,
            TrackTraversal traversal,
            JunctionNode   junction,
            Train          train) {

        if (track == null) {
            throw new IllegalArgumentException("Track is null");
        }
        if (traversal == null) {
            throw new IllegalArgumentException("TrackTraversal is null");
        }
        if (train == null) {
            throw new IllegalArgumentException("Train is null");
        }

        String trainId = train.getId();
        MovementContext context = new MovementContext(trainId,
            track.getId(), blockedBy);

        // 1. Track free?
        if (track.isInUse()) {
            Reservation active = track.getActiveReservation();
            String blockerId = active != null
                ? active.trainId() : "UNKNOWN";
            context.setBlockerTrainId(blockerId);
            if (headOn.isHeadOn(track, traversal)) {
                log("HEAD-ON CONFLICT", trainId, track.getId());
                blockedBy.put(trainId, blockerId);
                context.setDenialReason("HEAD_ON_CONFLICT");
                context.setSafeToProceed(false);
                return context;
            }
            if (following.isFollowing(track, traversal)) {
                log("FOLLOWING CONFLICT", trainId, track.getId());
                blockedBy.put(trainId, blockerId);
                context.setDenialReason("FOLLOWING_CONFLICT");
                context.setSafeToProceed(false);
                return context;
            }
        }

        // 2. Junction not isolated?
        if (junction != null && junction.isIsolated()) {
            log("JUNCTION ISOLATED", trainId, junction.getId());
            context.setJunctionLocked(true);
            context.setDenialReason("JUNCTION_ISOLATED");
            context.setSafeToProceed(false);
            return context;
        }

        // 2b. Topological junction movement validation (derailment blocks)
        if (junction != null && nextTrack != null
                && !isJunctionMovementAllowed(track, nextTrack, junction)) {
            log("DERAILMENT BLOCK", trainId, junction.getId());
            context.setDenialReason("DERAILMENT_BLOCK");
            context.setSafeToProceed(false);
            return context;
        }

        // 3. Deadlock check
        if (deadlock.hasCycle(trainId, blockedBy)) {
            log("DEADLOCK DETECTED", trainId, track.getId());
            context.setDenialReason("DEADLOCK");
            context.setSafeToProceed(false);
            return context;
        }

        // 4. Geometric overlap/fouling envelope check
        boolean overlapClear = computeOverlapEnvelope(track,
            nextTrack, junction, context);
        context.setOverlapClear(overlapClear);
        if (!overlapClear) {
            log("OVERLAP NOT CLEAR", trainId, track.getId());
            context.setDenialReason("OVERLAP_NOT_CLEAR");
            context.setSafeToProceed(false);
            return context;
        }

        // 5. TSR check — warn if speed limit changed
        if (track.hasTSR()) {
            log("TSR ACTIVE on " + track.getId()
                + " effective max: " + track.getEffectiveMaxSpeed()
                + " m/s", trainId, track.getId());
        }

        // All clear
        blockedBy.remove(trainId);
        context.setSafeToProceed(true);
        return context;
    }

    // Primary check. Returns true = safe to enter. false = hold train.
    public boolean check(
            Track          track,
            Track          nextTrack,
            TrackTraversal traversal,
            JunctionNode   junction,
            Train          train) {
        return assess(track, nextTrack, traversal, junction,
            train).isSafeToProceed();
    }

    // Called when a train exits a track — release junction isolation
    public void onTrackExit(Train train, Track track,
                             JunctionNode junction) {
        if (!train.isLastVehicleConfirmed()) {
            log("WARNING: last vehicle not confirmed for "
                + train.getId(), train.getId(), track.getId());
        }
        if (junction != null) {
            junction.releaseAfterClearance(train.getId(),
                track.getDistance());
        }
        track.release(train.getId());
        train.clearTrackOnUse();
    }

    // Called when a train enters a track — lock junction
    public void onTrackEntry(Train train, Track track,
                              TrackTraversal traversal,
                              JunctionNode junction) {
        // Lock the junction in its current mechanical state.
        if (junction != null) {
            junction.lockForRoute(train.getId());
        }

        long enterTime = System.currentTimeMillis() / 1000L;
        long occupancySeconds;
        if (track.getMinSpeedLimit() > 0 && track.getDistance() > 0) {
            occupancySeconds = (long) Math.ceil(
                track.getDistance() / track.getMinSpeedLimit());
        } else {
            occupancySeconds = 1L;
        }
        long expectedExitTime = enterTime + occupancySeconds;

        // Reserve the track with a single active temporal reservation.
        track.reserve(train.getId(), traversal.getDirection(),
            enterTime, expectedExitTime);
        train.setTrackOnUse(track.getId());
        train.resetLastVehicle();
    }

    private boolean computeOverlapEnvelope(Track currentTrack,
                                            Track nextTrack,
                                            JunctionNode junction,
                                            MovementContext context) {
        if (nextTrack == null) {
            context.setRequiredClearanceMetres(0.0);
            context.setAvailableClearanceMetres(0.0);
            return true;
        }

        double overlap = Math.max(0.0, currentTrack.getOverlapMetres());
        double fouling = 0.0;
        if (junction != null) {
            fouling = Math.max(0.0, junction.getFoulingDistanceMetres());
        }
        double required = Math.max(overlap, fouling);
        double available = nextTrack.isInUse() ? 0.0 : nextTrack.getDistance();

        context.setRequiredClearanceMetres(required);
        context.setAvailableClearanceMetres(available);

        return available >= required;
    }

    private boolean isJunctionMovementAllowed(Track incomingTrack,
                                              Track outgoingTrack,
                                              JunctionNode junction) {
        String junctionId = junction.getId();
        String incomingNode = otherEndId(incomingTrack, junctionId);
        String outgoingNode = otherEndId(outgoingTrack, junctionId);

        if (incomingNode == null || outgoingNode == null) {
            return true;
        }

        String facing = junction.getFacingPointer();
        String diverging = junction.getDivergingPointer();
        boolean state = junction.getState();

        if (incomingNode.equals(facing)) {
            if (state) {
                return outgoingNode.equals(diverging);
            }
            return !outgoingNode.equals(facing)
                && !outgoingNode.equals(diverging);
        }

        if (incomingNode.equals(diverging)) {
            if (!state) {
                return false;
            }
            return outgoingNode.equals(facing);
        }

        if (state) {
            return false;
        }
        return outgoingNode.equals(facing);
    }

    private String otherEndId(Track track, String nodeId) {
        String start = track.getStartNode().getId();
        String end = track.getEndNode().getId();
        if (start.equals(nodeId)) {
            return end;
        }
        if (end.equals(nodeId)) {
            return start;
        }
        return null;
    }

    private void log(String event, String trainId, String location) {
        System.out.println("[CONFLICT] "
            + java.time.LocalDateTime.now()
            + " | " + event
            + " | train: " + trainId
            + " | location: " + location);
    }
}
