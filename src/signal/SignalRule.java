package signal;

import conflict.DeadlockDetector;
import conflict.HeadOnConflict;
import core.MovementContext;
import model.*;

public final class SignalRule {

    // Condition 1: track not in use
    private boolean isTrackFree(Track track) {
        return !track.isInUse();
    }

    // Condition 2: no cycle in reservation graph
    private boolean isDeadlockFree(MovementContext context) {
        return !new DeadlockDetector().hasCycle(
            context.getTrainId(), context.getBlockedBy());
    }

    // Condition 3: no opposite direction train on track
    private boolean isDirectionClear(Track track,
                                     TrackTraversal traversal) {
        return !new HeadOnConflict().isHeadOn(track, traversal);
    }

    // Condition 4: overlap/fouling envelope is clear
    public boolean isOverlapClear(MovementContext context,
                                   Track currentTrack,
                                   Track nextTrack,
                                   JunctionNode junction) {
        if (context == null) {
            return false;
        }
        if (!context.isOverlapClear()) {
            return false;
        }
        return context.getAvailableClearanceMetres()
            >= context.getRequiredClearanceMetres();
    }

    // Condition 5: junction on route is not isolated by another train
    private boolean isJunctionAvailable(JunctionNode junction) {
        if (junction == null) return true;
        return !junction.isIsolated();
    }

    // All five conditions — call this for final GREEN decision
    public boolean canSetGreen(Track track,
                                Track nextTrack,
                                TrackTraversal traversal,
                                JunctionNode junction,
                                MovementContext context) {
        // IR interlocking principle: do not clear signal unless route checks pass.
        return isTrackFree(track)
            && isDirectionClear(track, traversal)
            && isOverlapClear(context, track, nextTrack, junction)
            && isJunctionAvailable(junction)
            && isDeadlockFree(context);
    }
}
