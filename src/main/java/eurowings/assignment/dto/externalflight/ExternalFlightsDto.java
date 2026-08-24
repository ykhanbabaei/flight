package eurowings.assignment.dto.externalflight;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExternalFlightsDto(

        String source,

        @JsonProperty("generated_at")
        long generatedAt,

        List<FlightOffer> results
) {
}