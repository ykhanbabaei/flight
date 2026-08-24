package eurowings.assignment.dto;

public record AlternativeFlightDto(
        String id,
        String trainOperator,
        String airline,
        String from,
        String to,
        String time,
        String duration,
        String price,
        String routeType
) {}
