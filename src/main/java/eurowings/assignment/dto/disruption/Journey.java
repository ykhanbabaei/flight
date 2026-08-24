package eurowings.assignment.dto.disruption;

import java.util.List;

public record Journey(
        String origin,
        String destination,
        List<FlightSegment> segments
) {
}