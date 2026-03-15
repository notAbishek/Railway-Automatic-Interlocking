package conflict;

import model.Track;
import model.TrackTraversal;

public final class FollowingConflict {

    // Returns true if a same-direction train occupies this track
    public boolean isFollowing(Track track, TrackTraversal traversal) {
        if (!track.isInUse()) return false;
        if (track.getOccupiedDirection() == null) return false;
        return track.getOccupiedDirection() == traversal.getDirection();
    }
}
