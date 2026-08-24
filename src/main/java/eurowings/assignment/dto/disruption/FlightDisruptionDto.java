package eurowings.assignment.dto.disruption;

import java.util.List;

public record FlightDisruptionDto(
        Disruption disruption,
        List<Booking> bookings
) {
}