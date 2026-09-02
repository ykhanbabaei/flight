package com.yousef.flightrecovery.controler;

public record ErrorResponse(
        int status,
        String message
) {
}