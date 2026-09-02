package com.yousef.flightrecovery.dto.internalflight;

public record InternalFlightLeg(
        String flightNumber,
        String from,
        String to
) {
}