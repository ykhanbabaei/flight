package com.yousef.flightrecovery.dto.externalflight;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record FlightOffer(
        @JsonProperty("offer_id")
        String offerId,

        @JsonProperty("airline_name")
        String airlineName,

        @JsonProperty("dep_airport")
        String departureAirport,

        @JsonProperty("arr_airport")
        String arrivalAirport,

        @JsonProperty("dep_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime departureTime,

        @JsonProperty("arr_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime arrivalTime,

        int availability,

        @JsonProperty("price_cents")
        int priceCents,

        String currency,

        List<FlightLeg> legs,

        String note
) {
}