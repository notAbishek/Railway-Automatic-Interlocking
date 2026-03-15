package model;

import enums.Direction;

public final class TrackTraversal {
    private final Track     track;
    private final Direction direction;  // FORWARD or REVERSE, derived by PathFinder

    public TrackTraversal(Track track, Direction direction) {
        this.track     = track;
        this.direction = direction;
    }

    public Track     getTrack()     { return track; }
    public Direction getDirection() { return direction; }

    public String getTrackId()      { return track.getId(); }

    @Override
    public String toString() {
        return track.getId() + " [" + direction + "]";
    }
}
