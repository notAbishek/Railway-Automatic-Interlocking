package conflict;

import model.Reservation;
import model.Track;
import model.TrackTraversal;

public final class FollowingConflict {

    // Returns true if a same-direction train occupies this track
    public boolean isFollowing(Track track, TrackTraversal traversal) {
        Reservation reservation = track.getActiveReservation();
        if (reservation == null) return false;
        return reservation.direction() == traversal.getDirection();
    }
}
