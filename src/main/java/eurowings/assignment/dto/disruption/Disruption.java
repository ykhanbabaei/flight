package eurowings.assignment.dto.disruption;

import java.time.OffsetDateTime;

public record Disruption(
        String flight,
        String origin,
        String destination,
        OffsetDateTime scheduledDeparture,
        OffsetDateTime scheduledArrival,
        String status,
        String reason,
        OffsetDateTime cancelledAt,
        int affectedBookings,
        int affectedPassengers
) {
}