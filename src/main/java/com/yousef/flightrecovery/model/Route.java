package com.yousef.flightrecovery.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Route(
        String id,
        RouteType routeType,
        String fromAirport,
        String toAirport,
        OffsetDateTime departureDateTime,
        OffsetDateTime arrivalDateTime,
        int availableSeats,
        BigDecimal price,
        String airlineName,
        String trainOperator
) { }
