package model;

import enums.Direction;

public record Reservation(
        String trainId,
        Direction direction,
        long enterTime,
        long expectedExitTime) {

    public Reservation {
        if (trainId == null || trainId.isEmpty()) {
            throw new IllegalArgumentException("trainId cannot be null or empty");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        if (expectedExitTime < enterTime) {
            throw new IllegalArgumentException(
                "expectedExitTime cannot be earlier than enterTime");
        }
    }
}