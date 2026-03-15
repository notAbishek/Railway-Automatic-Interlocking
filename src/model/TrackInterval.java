package model;

import enums.Direction;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TrackInterval {
    private final String        trainId;
    private final String        trackId;
    private LocalDateTime enterTime;
    private LocalDateTime exitTime;
    private final Direction     direction;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public TrackInterval(String trainId, String trackId, LocalDateTime enterTime, LocalDateTime exitTime, Direction direction) {
        this.trainId   = trainId;
        this.trackId   = trackId;
        this.enterTime = enterTime;
        this.exitTime  = exitTime;
        this.direction = direction;
    }

    // Push this interval forward — used by MeetAndPassResolver
    public void delay(Duration duration) {
        this.enterTime = this.enterTime.plus(duration);
        this.exitTime  = this.exitTime.plus(duration);
    }

    // How long does this train occupy this track?
    public Duration getOccupancyDuration() {
        return Duration.between(enterTime, exitTime);
    }

    public String        getTrainId()   { return trainId; }
    public String        getTrackId()   { return trackId; }
    public LocalDateTime getEnterTime() { return enterTime; }
    public LocalDateTime getExitTime()  { return exitTime; }
    public Direction     getDirection() { return direction; }

    @Override
    public String toString() {
        return trainId + " on " + trackId
            + " [" + enterTime.format(FMT)
            + " → " + exitTime.format(FMT) + "]"
            + " " + direction;
    }
}