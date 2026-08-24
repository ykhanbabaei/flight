package eurowings.assignment.utils;

import eurowings.assignment.dto.AlternativeFlightDto;
import eurowings.assignment.dto.FlightBookingDto;
import eurowings.assignment.dto.FlightDisruptionDto;
import eurowings.assignment.dto.disruption.FlightDisruptionResponse;
import eurowings.assignment.dto.disruption.FlightSegment;
import eurowings.assignment.dto.disruption.Journey;
import eurowings.assignment.model.Route;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

public class FlightEntityMapper {

    private FlightEntityMapper() {}

    public static FlightDisruptionDto toFlightBookingDtoList(FlightDisruptionResponse flightDisruption, Map<String, List<AlternativeFlightDto>> flightAlternatives) {
        return new FlightDisruptionDto(
            flightDisruption.disruption().flight(),
            flightDisruption.disruption().origin(),
            flightDisruption.disruption().destination(),
            flightDisruption.disruption().scheduledDeparture(),
            flightDisruption.disruption().scheduledArrival(),
            flightDisruption.disruption().status(),
            flightDisruption.disruption().reason(),
            flightDisruption.disruption().cancelledAt(),
            flightDisruption.disruption().affectedBookings(),
            flightDisruption.disruption().affectedPassengers(),
            flightDisruption.bookings().stream().map(booking -> new FlightBookingDto(
                booking.bookingRef(),
                booking.passengers(),
                flightDisruption.disruption().flight(),
                getRoute(booking.journey()),
                flightDisruption.disruption().scheduledDeparture().toString(),
                flightDisruption.disruption().status(),
                flightAlternatives.getOrDefault(booking.bookingRef(), Collections.emptyList())
            )
        ).toList()
        );
    }

    private static String getRoute(Journey journey) {
        var route = new LinkedHashSet<String>();
        route.add(journey.origin());
        for(FlightSegment segment : journey.segments()) {
            route.add(segment.from());
            route.add(segment.to());
        }
        route.add(journey.destination());
        return String.join(" → ", route);
    }

    public static List<AlternativeFlightDto> toAlternativeFlightDtoList(List<Route> routes) {
        return routes.stream().map(route -> new AlternativeFlightDto(
                route.id(),
                route.trainOperator(),
                route.airlineName(),
                route.fromAirport(),
                route.toAirport(),
                route.departureDateTime().toString(),
                flightDuration(route),
                route.price().toPlainString(),
                route.routeType().name())).toList();
    }

    private static String flightDuration(Route route) {
        OffsetDateTime arrival = OffsetDateTime.parse(route.arrivalDateTime().toString());
        OffsetDateTime departure = OffsetDateTime.parse(route.departureDateTime().toString());
        Duration duration = Duration.between(departure, arrival);
        return duration.toHours() + "h " + (duration.toMinutes() % 60) + "m";
    }
}

