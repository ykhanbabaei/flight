package eurowings.assignment.dto.internalflight;

public record InternalFlightLeg(
        String flightNumber,
        String from,
        String to
) {
}