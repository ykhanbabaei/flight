package com.yousef.flightrecovery.dto.externalflight;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExternalFlightsResponse(

        String source,

        @JsonProperty("generated_at")
        long generatedAt,

        List<FlightOffer> results
) {
}