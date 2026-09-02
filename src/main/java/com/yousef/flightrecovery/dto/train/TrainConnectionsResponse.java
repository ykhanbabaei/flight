package com.yousef.flightrecovery.dto.train;

import java.util.List;

public record TrainConnectionsResponse(
        String provider,
        List<RailQuery> queries
) {
}