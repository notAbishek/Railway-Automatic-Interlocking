package signal;

import conflict.DeadlockDetector;
import conflict.HeadOnConflict;
import java.util.Map;
import model.*;

public final class SignalRule {

    // Condition 1: track not in use
    public boolean isTrackFree(Track track) {
        return !track.isInUse() && track.getUsedBy().isEmpty();
    }

    // Condition 2: no cycle in reservation graph
    public boolean isDeadlockFree(String trainId,
                                   Map<String, String> blockedBy) {
        return !new DeadlockDetector().hasCycle(trainId, blockedBy);
    }

    // Condition 3: no opposite direction train on track
    public boolean isDirectionClear(Track track,
                                     TrackTraversal traversal) {
        return !new HeadOnConflict().isHeadOn(track, traversal);
    }

    // Condition 4: overlap zone (180m ahead) is clear
    // For V1 — check the NEXT track in the path is also free
    public boolean isOverlapClear(Track currentTrack,
                                   Track nextTrack) {
        if (nextTrack == null) return true;  // last track in path
        return !nextTrack.isInUse();
    }

    // Condition 5: junction on route is not isolated by another train
    public boolean isJunctionAvailable(JunctionNode junction) {
        if (junction == null) return true;
        return !junction.isIsolated();
    }

    // All five conditions — call this for final GREEN decision
    public boolean canSetGreen(Track track,
                                Track nextTrack,
                                TrackTraversal traversal,
                                JunctionNode junction,
                                String trainId,
                                Map<String, String> blockedBy) {
        // IR interlocking principle: do not clear signal unless route checks pass.
        return isTrackFree(track)
            && isDirectionClear(track, traversal)
            && isOverlapClear(track, nextTrack)
            && isJunctionAvailable(junction)
            && isDeadlockFree(trainId, blockedBy);
    }
}
