package eurowings.assignment.dto.internalflight;

import java.time.OffsetDateTime;

public record FlightSearchQuery(
        String origin,
        String destination,
        OffsetDateTime earliestDeparture
) {
}