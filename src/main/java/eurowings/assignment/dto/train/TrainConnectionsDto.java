package eurowings.assignment.dto.train;

import java.util.List;

public record TrainConnectionsDto(
        String provider,
        List<RailQuery> queries
) {
}