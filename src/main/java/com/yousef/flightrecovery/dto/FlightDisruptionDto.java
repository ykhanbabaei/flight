package com.yousef.flightrecovery.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FlightDisruptionDto(
        String flight,
        String origin,
        String destination,
        OffsetDateTime scheduledDeparture,
        OffsetDateTime scheduledArrival,
        String status,
        String reason,
        OffsetDateTime cancelledAt,
        int affectedBookings,
        int affectedPassengers,
        List<FlightBookingDto> flightBookings
) {
}
