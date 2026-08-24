package eurowings.assignment.dto.internalflight;

import java.time.OffsetDateTime;
import java.util.List;

public record InternalFlightsResponse(
        String source,
        OffsetDateTime generatedAt,
        List<FlightSearch> searches
) {
}