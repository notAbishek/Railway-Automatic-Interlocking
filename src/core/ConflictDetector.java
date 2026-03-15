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
        // Set junction state based on which route this train is taking
        if (junction != null && junction.getDirection()
                == enums.JunctionDirection.LEFT) {
            // This is a SPLIT junction — determine which exit this train uses
            // We need to know the next track's node to decide primary vs secondary
            // For V1: derive from the track being entered after this junction
            // The track being reserved IS the outgoing track from the junction
            // Check if this track leads to the primary or secondary node

            String trackEndId = track.getEndNode().getId();
            if (trackEndId.equals(junction.getSecondaryNodeId())) {
                junction.setState(true);   // secondary route
            } else {
                junction.setState(false);  // primary route (default)
            }
        }

        if (junction != null && junction.getDirection()
                == enums.JunctionDirection.RIGHT) {
            // This is a MERGE junction — record which incoming side
            String trackStartId = track.getStartNode().getId();
            if (trackStartId.equals(junction.getSecondaryNodeId())) {
                junction.setState(true);
            } else {
                junction.setState(false);
            }
        }

        // Lock the junction AFTER state is set
        if (junction != null) {
            junction.isolate(train.getId());
        }

        // Reserve the track (already exists — keep it)
        track.reserve(train.getId(), traversal.getDirection());
        train.setTrackOnUse(track.getId());
        train.resetLastVehicle();
    }

    private void log(String event, String trainId, String location) {
        System.out.println("[CONFLICT] "
            + java.time.LocalDateTime.now()
            + " | " + event
            + " | train: " + trainId
            + " | location: " + location);
    }
}
