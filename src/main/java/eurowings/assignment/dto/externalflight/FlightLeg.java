package eurowings.assignment.dto.externalflight;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FlightLeg(
        @JsonProperty("flight_no")
        String flightNumber,

        String from,
        String to
) {
}