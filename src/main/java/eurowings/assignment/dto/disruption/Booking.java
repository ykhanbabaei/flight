package eurowings.assignment.dto.disruption;


public record Booking(
        String bookingRef,
        int passengers,
        Journey journey
) {
}