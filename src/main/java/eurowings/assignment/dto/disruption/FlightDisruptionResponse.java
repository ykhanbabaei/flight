package eurowings.assignment.dto.disruption;

import java.util.List;

public record FlightDisruptionResponse(
        Disruption disruption,
        List<Booking> bookings
) {
}