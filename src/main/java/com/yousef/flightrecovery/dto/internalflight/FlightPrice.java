package com.yousef.flightrecovery.dto.internalflight;

import java.math.BigDecimal;

public record FlightPrice(
        BigDecimal amount,
        String currency
) {
}