package conflict;

import model.Track;
import model.TrackTraversal;

public final class HeadOnConflict {

    // Returns true if an opposite-direction train is currently on this track
    public boolean isHeadOn(Track track, TrackTraversal traversal) {
        if (!track.isInUse()) return false;
        if (track.getOccupiedDirection() == null) return false;
        return track.getOccupiedDirection() != traversal.getDirection();
    }
}
