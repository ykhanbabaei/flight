package com.yousef.flightrecovery.dto.train;

import java.time.LocalDateTime;

public record TrainConnection(
        String id,
        String operator,
        String operatorCode,
        String trainNumber,
        String departureStation,
        String arrivalStation,
        LocalDateTime departure,
        LocalDateTime arrival,
        int availableSeats,
        String price,
        String via
) {
}