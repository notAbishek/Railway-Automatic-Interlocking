package core;

import conflict.DeadlockDetector;
import conflict.FollowingConflict;
import conflict.HeadOnConflict;
import java.util.*;
import model.*;
import signal.SignalRule;

public class ConflictDetector {

    private final HeadOnConflict      headOn    = new HeadOnConflict();
    private final FollowingConflict   following = new FollowingConflict();
    private final DeadlockDetector    deadlock  = new DeadlockDetector();
    private final SignalRule          rule      = new SignalRule();
    private final Map<String, String> blockedBy = new HashMap<>();

    // Primary check. Returns true = safe to enter. false = hold train.
    public boolean check(
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

        // 1. Track free?
        if (track.isInUse()) {
            if (headOn.isHeadOn(track, traversal)) {
                log("HEAD-ON CONFLICT", trainId, track.getId());
                blockedBy.put(trainId, track.getUsedBy().get(0));
                return false;
            }
            if (following.isFollowing(track, traversal)) {
                log("FOLLOWING CONFLICT", trainId, track.getId());
                blockedBy.put(trainId, track.getUsedBy().get(0));
                return false;
            }
        }

        // 2. Junction not isolated?
        if (junction != null && junction.isIsolated()) {
            log("JUNCTION ISOLATED", trainId, junction.getId());
            return false;
        }

        // 2b. Topological junction movement validation (derailment blocks)
        if (junction != null && nextTrack != null
                && !isJunctionMovementAllowed(track, nextTrack, junction)) {
            log("DERAILMENT BLOCK", trainId, junction.getId());
            return false;
        }

        // 3. Deadlock check
        if (deadlock.hasCycle(trainId, blockedBy)) {
            log("DEADLOCK DETECTED", trainId, track.getId());
            return false;
        }

        // 4. Overlap clear?
        if (!rule.isOverlapClear(track, nextTrack)) {
            log("OVERLAP NOT CLEAR", trainId, track.getId());
            return false;
        }

        // 5. TSR check — warn if speed limit changed
        if (track.hasTSR()) {
            log("TSR ACTIVE on " + track.getId()
                + " effective max: " + track.getEffectiveMaxSpeed()
                + " m/s", trainId, track.getId());
        }

        // All clear
        blockedBy.remove(trainId);
        return true;
    }

    // Called when a train exits a track — release junction isolation
    public void onTrackExit(Train train, Track track,
                             JunctionNode junction) {
        if (!train.isLastVehicleConfirmed()) {
            log("WARNING: last vehicle not confirmed for "
                + train.getId(), train.getId(), track.getId());
        }
        if (junction != null) {
            junction.release(train.getId());
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
            junction.isolate(train.getId());
        }

        // Reserve the track (already exists — keep it)
        track.reserve(train.getId(), traversal.getDirection());
        train.setTrackOnUse(track.getId());
        train.resetLastVehicle();
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
