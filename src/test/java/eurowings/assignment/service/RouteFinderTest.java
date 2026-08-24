package eurowings.assignment.service;

import eurowings.assignment.model.Route;
import eurowings.assignment.model.RouteType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RouteFinderTest {

    @Test
    void shouldFindBestRoute() throws IOException {
        //Optimal way is:
        // "from": "CGN",
        // "to":  "IST" ,
        // "totalPriceCents": 249,
        List<Route> routes = readMockRoutes();
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Route> optimalRoutes = RouteFinder.findBestRoutesForJourney("CGN", "IST", OffsetDateTime.MAX, 3, routes, remainingSeats);
        assertEquals(1, optimalRoutes.size());
        assertEquals(BigDecimal.valueOf(249), optimalRoutes.getFirst().price());

    }

    @Test
    void shouldFindRoutesWithConformingDeadline() throws IOException {
        //Optimal way is:
        // "from": "CGN" ,
        // "to":  "IST" ,
        // "totalPriceCents": 24900,
        List<Route> routes = readMockRoutes();
        OffsetDateTime deadline = OffsetDateTime.parse("2026-07-21T16:15:00+03:00");
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Route> optimalRoutes = RouteFinder.findBestRoutesForJourney("CGN", "IST", deadline, 3, routes, remainingSeats);
        assertEquals(1, optimalRoutes.size());
        assertEquals("CGN", optimalRoutes.getFirst().fromAirport());
        assertEquals("IST", optimalRoutes.getFirst().toAirport());
        assertEquals(BigDecimal.valueOf(249), optimalRoutes.getFirst().price());
        assertTrue(optimalRoutes.getFirst().arrivalDateTime().isBefore(deadline));
    }

    @Test
    void shouldNotFindRoutesBecauseUnreachableDeadline() throws IOException {
        //Optimal way is:
        // NONE, not possible without missing deadline
        List<Route> routes = readMockRoutes();
        OffsetDateTime deadline = OffsetDateTime.parse("2026-07-20T21:15:00+03:00");
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Route> optimalRoutes = RouteFinder.findBestRoutesForJourney("CGN", "IST", deadline, 3, routes, remainingSeats);
        assertEquals(0, optimalRoutes.size());
    }

    @Test
    void shouldNotFindRoutesWhenAirportNotSupported() throws IOException {
        //Optimal way is:
        // NONE, not possible without missing deadline
        List<Route> routes = readMockRoutes();
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Route> optimalRoutes = RouteFinder.findBestRoutesForJourney("CGN", "DOH", OffsetDateTime.MAX, 3, routes, remainingSeats);
        assertEquals(0, optimalRoutes.size());
    }

    @Test
    void shouldNotFindRoutesWhenTooManyPassengers() throws IOException {
        //Optimal way is:
        // NONE, not possible without missing deadline
        List<Route> routes = readMockRoutes();
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Route> optimalRoutes = RouteFinder.findBestRoutesForJourney("CGN", "IST", OffsetDateTime.MAX, 150, routes, remainingSeats);
        assertEquals(0, optimalRoutes.size());
    }

    private List<Route> readMockRoutes() throws IOException {
        var resource = new ClassPathResource("optimal-routes-finder.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>(){});
        }
    }

    @Test
    void shouldUseRouteWhenExactlyEnoughSeatsAreAvailable() {
        Route route = route(
                "R1",
                "CGN",
                "IST",
                10,
                100
        );

        Map<String, Integer> remainingSeats = Map.of(
                "R1", 3
        );

        List<Route> result = RouteFinder.findBestRoutesForJourney(
                "CGN",
                "IST",
                OffsetDateTime.MAX,
                3,
                List.of(route),
                remainingSeats
        );

        assertEquals(1, result.size());
        assertEquals("R1", result.getFirst().id());
    }

    private Route route(String id, String from, String to, int availableSeats, int price) {
        OffsetDateTime departure =
                OffsetDateTime.parse("2026-08-24T10:00:00+02:00");
        return new Route(id, RouteType.FLIGHT, from, to, departure, departure.plusHours(3),
                availableSeats, BigDecimal.valueOf(price), null, null);
    }

    @Test
    void shouldNotUseRouteWhenAvailableSeatsAreLessThanPassengers() {
        Route route = route(
                "R1",
                "CGN",
                "IST",
                10,
                100
        );

        Map<String, Integer> remainingSeats = Map.of(
                "R1", 2
        );

        List<Route> result = RouteFinder.findBestRoutesForJourney(
                "CGN",
                "IST",
                OffsetDateTime.MAX,
                3,
                List.of(route),
                remainingSeats
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldChooseAlternativeRouteWhenCheapestRouteHasInsufficientSeats() {
        Route cheapRoute = route(
                "CHEAP",
                "CGN",
                "IST",
                2,
                100
        );

        Route moreExpensiveRoute = route(
                "EXPENSIVE",
                "CGN",
                "IST",
                10,
                200
        );

        Map<String, Integer> remainingSeats = Map.of(
                "CHEAP", 2,
                "EXPENSIVE", 10
        );

        List<Route> result = RouteFinder.findBestRoutesForJourney(
                "CGN",
                "IST",
                OffsetDateTime.MAX,
                3,
                List.of(cheapRoute, moreExpensiveRoute),
                remainingSeats
        );

        assertEquals(1, result.size());
        assertEquals("EXPENSIVE", result.getFirst().id());
    }



}