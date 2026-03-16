package conflict;

import java.time.Duration;
import java.time.LocalDateTime;
import model.TrackInterval;

public final class TimeWindowConflict {

    // Returns true if two TrackIntervals conflict:
    // same track + any time overlap (absolute block exclusivity)
    public boolean hasConflict(TrackInterval a, TrackInterval b) {
        if (!a.getTrackId().equals(b.getTrackId())) return false;
        return a.getEnterTime().isBefore(b.getExitTime())
            && b.getEnterTime().isBefore(a.getExitTime());
    }

    // Returns [conflictStart, conflictEnd] as LocalDateTime[2]
    // Only call if hasConflict() returned true
    public LocalDateTime[] getConflictWindow(TrackInterval a,
                                              TrackInterval b) {
        LocalDateTime start = a.getEnterTime().isAfter(b.getEnterTime())
            ? a.getEnterTime() : b.getEnterTime();
        LocalDateTime end   = a.getExitTime().isBefore(b.getExitTime())
            ? a.getExitTime()  : b.getExitTime();
        return new LocalDateTime[]{ start, end };
    }

    // How many seconds must b be delayed so it no longer conflicts with a?
    // Returns 0 if no conflict
    public long getRequiredDelaySeconds(TrackInterval a, TrackInterval b) {
        if (!hasConflict(a, b)) return 0;
        LocalDateTime conflictEnd = getConflictWindow(a, b)[1];
        if (b.getEnterTime().isBefore(conflictEnd)) {
            return Duration.between(b.getEnterTime(), conflictEnd).getSeconds() + 1;
        }
        return 0;
    }
}
