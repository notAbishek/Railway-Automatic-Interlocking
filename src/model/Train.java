package model;

import enums.*;
import java.time.LocalDateTime;

public class Train {
    private final String id;
    private final String name;
    private final TrainType type;
    private final TrainPriority priority;
    private String trackOnUse;
    private String startNode;
    private String endNode;
    private double speed;
    private LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private LocalDateTime actualArrivalTime;
    private double        delayHours;
    private int     coachCount             = 0;
    private boolean lastVehicleConfirmed   = false;
    // set to true when the LV marker of this train passes
    // a station or signal cabin. If false after train clears
    // a block, assume train has parted — trigger emergency.

    public Train(
        String id,
        String name,
        TrainType type,
        TrainPriority priority,
        String trackOnUse,
        String startNode,
        String endNode,
        double speed,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
    ) {
        this.id            = id;
        this.name          = name;
        this.type          = type;
        this.priority      = priority;
        this.trackOnUse    = trackOnUse;
        this.startNode     = startNode;
        this.endNode       = endNode;
        this.speed         = speed;
        this.departureTime = departureTime;
        this.arrivalTime   = arrivalTime;
    }

    public String        getId()            { return this.id; }
    public String        getName()          { return this.name; }
    public TrainType     getType()          { return this.type; }
    public TrainPriority getPriority()      { return this.priority; }
    public String        getTrackOnUse()    { return this.trackOnUse; }
    public String        getStartNode()     { return this.startNode; }
    public String        getEndNode()       { return this.endNode; }
    public double        getSpeed()         { return this.speed; }
    public LocalDateTime getDepartureTime() { return this.departureTime; }
    public LocalDateTime getArrivalTime()   { return this.arrivalTime; }
    public LocalDateTime getActualArrivalTime() { return this.actualArrivalTime; }
    public double        getDelayHours()    { return this.delayHours; }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public void setActualArrivalTime(LocalDateTime actualArrival) {
        this.actualArrivalTime = actualArrival;

        // Calculate delay against planner's scheduled arrivalTime
        if (this.arrivalTime != null && actualArrival.isAfter(this.arrivalTime)) {
            long delaySeconds = java.time.Duration.between(
                this.arrivalTime, actualArrival).getSeconds();
            this.delayHours = delaySeconds / 3600.0;
        } else {
            this.delayHours = 0;
        }
    }

    public void setTrackOnUse(String trackId)  { this.trackOnUse = trackId; }
    public void clearTrackOnUse()              { this.trackOnUse = null; }

    public int     getCoachCount()           { return coachCount; }
    public void    setCoachCount(int n)      { this.coachCount = n; }
    public boolean isLastVehicleConfirmed()  { return lastVehicleConfirmed; }
    public void    confirmLastVehicle()      { this.lastVehicleConfirmed = true; }
    public void    resetLastVehicle()        { this.lastVehicleConfirmed = false; }

}