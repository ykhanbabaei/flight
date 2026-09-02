package com.yousef.flightrecovery.dto.internalflight;

import java.time.OffsetDateTime;
import java.util.List;

public record InternalFlightOffer(
        String offerId,
        String carrier,
        String carrierCode,
        String departureAirport,
        String arrivalAirport,
        OffsetDateTime departure,
        OffsetDateTime arrival,
        int availableSeats,
        FlightPrice pricePerSeat,
        List<InternalFlightLeg> legs,
        String note
) {
}