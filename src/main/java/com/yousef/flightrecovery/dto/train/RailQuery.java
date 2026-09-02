package com.yousef.flightrecovery.dto.train;

import java.time.LocalDateTime;
import java.util.List;

public record RailQuery(
        String fromCity,
        String fromAirport,
        String toCity,
        String toAirport,
        LocalDateTime notBefore,
        List<TrainConnection> connections
) {
}