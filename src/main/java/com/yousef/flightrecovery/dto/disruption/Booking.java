package com.yousef.flightrecovery.dto.disruption;


public record Booking(
        String bookingRef,
        int passengers,
        Journey journey
) {
}