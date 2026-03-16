package conflict;

import model.Reservation;
import model.Track;
import model.TrackTraversal;

public final class HeadOnConflict {

    // Returns true if an opposite-direction train is currently on this track
    public boolean isHeadOn(Track track, TrackTraversal traversal) {
        Reservation reservation = track.getActiveReservation();
        if (reservation == null) return false;
        return reservation.direction() != traversal.getDirection();
    }
}
