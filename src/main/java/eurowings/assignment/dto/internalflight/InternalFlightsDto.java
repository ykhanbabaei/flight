package eurowings.assignment.dto.internalflight;

import java.time.OffsetDateTime;
import java.util.List;

public record InternalFlightsDto(
        String source,
        OffsetDateTime generatedAt,
        List<FlightSearch> searches
) {
}