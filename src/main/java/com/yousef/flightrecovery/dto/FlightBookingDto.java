package com.yousef.flightrecovery.dto;

import java.util.List;

public record FlightBookingDto(
        String id,
        Integer passengerCount,
        String flight,
        String route,
        String departure,
        String status,  // Using enum instead of string literal
        List<AlternativeFlightDto> alternatives
) {}