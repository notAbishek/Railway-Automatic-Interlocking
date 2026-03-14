package model;

import enums.Direction;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TrackInterval {
    private String        trainId;
    private String        trackId;
    private LocalDateTime enterTime;
    private LocalDateTime exitTime;
    private Direction     direction;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public TrackInterval(String trainId, String trackId, LocalDateTime enterTime, LocalDateTime exitTime, Direction direction) {
        this.trainId   = trainId;
        this.trackId   = trackId;
        this.enterTime = enterTime;
        this.exitTime  = exitTime;
        this.direction = direction;
    }


    public boolean conflictsWith(TrackInterval other) {
        if (!this.trackId.equals(other.trackId)){
            return false;
        }
        if (this.direction == other.direction){
                return false;
        }
        return this.enterTime.isBefore(other.exitTime)
            && other.enterTime.isBefore(this.exitTime);
    }

    public LocalDateTime[] getConflictWindow(TrackInterval other) {
        LocalDateTime start = this.enterTime.isAfter(other.enterTime)
            ? this.enterTime : other.enterTime;
        LocalDateTime end   = this.exitTime.isBefore(other.exitTime)
            ? this.exitTime  : other.exitTime;
        return new LocalDateTime[]{ start, end };
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