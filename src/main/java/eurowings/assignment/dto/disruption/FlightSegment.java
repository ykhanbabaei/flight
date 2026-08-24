package eurowings.assignment.dto.disruption;

import java.time.OffsetDateTime;

public record FlightSegment(
        String flight,
        String from,
        String to,
        OffsetDateTime departure,
        OffsetDateTime arrival,
        FlightStatus status
) {
}