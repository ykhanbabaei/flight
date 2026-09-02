package com.yousef.flightrecovery.service;

import com.yousef.flightrecovery.datasource.FlightDataSource;
import com.yousef.flightrecovery.datasource.RoutesClient;
import com.yousef.flightrecovery.dto.FlightBookingDto;
import com.yousef.flightrecovery.dto.FlightDisruptionDto;
import com.yousef.flightrecovery.dto.disruption.*;
import com.yousef.flightrecovery.model.Route;
import com.yousef.flightrecovery.model.RouteType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlightDisruptionServiceTest {

    @Test
    void shouldSynchronizeCallbacksWhenProvidersCompleteAtSameTime() throws IOException {
        FlightDataSource flightDataSource = mock(FlightDataSource.class);

        //split routes to 3 providers
        var allRoutes = readMockRoutes();
        int size = allRoutes.size();
        int firstSplit = size / 3;
        int secondSplit = firstSplit * 2;
        var firstRoutes = allRoutes.subList(0, firstSplit);
        var secondRoutes = allRoutes.subList(firstSplit, secondSplit);
        var thirdRoutes = allRoutes.subList(secondSplit, size);

        RoutesClient firstProvider = mock(RoutesClient.class);
        RoutesClient secondProvider = mock(RoutesClient.class);
        RoutesClient thirdProvider = mock(RoutesClient.class);

        when(firstProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()-> firstRoutes));
        when(secondProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()-> secondRoutes));
        when(thirdProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()-> thirdRoutes));

        when(flightDataSource.getRoutesProviders()).thenReturn(List.of(firstProvider, secondProvider, thirdProvider));

        FlightDisruptionService service = new FlightDisruptionService(flightDataSource);

        AtomicInteger activeCallbacks = new AtomicInteger(0);
        AtomicInteger maxConcurrentCallbacks = new AtomicInteger(0);

        CompletableFuture<List<Route>> result = service.findAlternativesAsync(routes -> {
            int current = activeCallbacks.incrementAndGet();
            maxConcurrentCallbacks.accumulateAndGet(current, Math::max);

            try {
                Thread.sleep(100); // widen the overlap window
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                activeCallbacks.decrementAndGet();
            }
        });

        List<Route> mergedRoutes = result.join();

        assertEquals(allRoutes.size(), mergedRoutes.size());
        assertEquals(1, maxConcurrentCallbacks.get(),
                "The lock should serialize callback execution when both async providers finish at the same time.");
    }

    @Test
    void shouldContinueWhenAnyRoutesProviderFails() throws IOException {
        FlightDataSource flightDataSource = mock(FlightDataSource.class);

        //split routes to 3 providers
        var allRoutes = readMockRoutes();
        int size = allRoutes.size();
        int firstSplit = size / 3;
        int secondSplit = firstSplit * 2;
        var firstRoutes = allRoutes.subList(0, firstSplit);
        //var secondRoutes = allRoutes.subList(firstSplit, secondSplit);
        var thirdRoutes = allRoutes.subList(secondSplit, size);

        RoutesClient firstProvider = mock(RoutesClient.class);
        RoutesClient secondProvider = mock(RoutesClient.class);
        RoutesClient thirdProvider = mock(RoutesClient.class);

        when(firstProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()-> firstRoutes));
        when(secondProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()->{throw new RuntimeException("Cannot load routes from json file");}));
        when(thirdProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()->thirdRoutes));

        when(flightDataSource.getRoutesProviders()).thenReturn(List.of(firstProvider, secondProvider, thirdProvider));

        FlightDisruptionService service = new FlightDisruptionService(flightDataSource);
        var emittedRoutes = new AtomicReference<List<Route>>();
        CompletableFuture<List<Route>> routesFuture = service.findAlternativesAsync(accumulatedRoutes -> {

            try {
                Thread.sleep(100); // widen the overlap window
                emittedRoutes.set(accumulatedRoutes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        routesFuture.whenComplete((result, ex) -> assertNotNull(ex));

        try {
            routesFuture.join();
        } catch (Exception ignored) {}
        assertEquals(firstRoutes.size() + thirdRoutes.size(), emittedRoutes.get().size());
    }


    @Test
    void shouldTransmitDataWhenAnyProviderIsReady() throws IOException {
        FlightDataSource flightDataSource = mock(FlightDataSource.class);

        //split routes to 3 providers
        var allRoutes = readMockRoutes();
        int size = allRoutes.size();
        int firstSplit = size / 3;
        int secondSplit = firstSplit * 2;
        //var firstRoutes = allRoutes.subList(0, firstSplit);
        //var secondRoutes = allRoutes.subList(firstSplit, secondSplit);
        var thirdRoutes = allRoutes.subList(secondSplit, size);

        RoutesClient firstProvider = mock(RoutesClient.class);
        RoutesClient secondProvider = mock(RoutesClient.class);
        RoutesClient thirdProvider = mock(RoutesClient.class);

        when(firstProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()-> {throw new RuntimeException("Cannot load routes from first provider");}));
        when(secondProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()->{throw new RuntimeException("Cannot load routes from second provider");}));
        when(thirdProvider.fetchRoutes()).thenReturn(CompletableFuture.supplyAsync(()->thirdRoutes));

        when(flightDataSource.getRoutesProviders()).thenReturn(List.of(firstProvider, secondProvider, thirdProvider));

        FlightDisruptionService service = new FlightDisruptionService(flightDataSource);
        var emittedRoutes = new AtomicReference<List<Route>>();
        CompletableFuture<List<Route>> routesFuture = service.findAlternativesAsync(emittedRoutes::set);

        try {
            routesFuture.join();
        } catch (Exception ignored) {}
        assertEquals( thirdRoutes.size(), emittedRoutes.get().size());
    }

    @Test
    void shouldReturnOptionalFlightWhenDisruptionExists() {
        FlightDisruptionResponse expectedDisruption = createMockFlightDisruption();

        FlightDataSource flightDataSource = mock(FlightDataSource.class);
        when(flightDataSource.fetchFlightDisruption(any(), any())).thenReturn(Optional.of(expectedDisruption));

        FlightDisruptionService service = new FlightDisruptionService(flightDataSource);

        Optional<FlightDisruptionResponse> result = service.findFlightDisruption("ab123", OffsetDateTime.now());

        assertTrue(result.isPresent());
        assertEquals(expectedDisruption, result.get());
    }

    @Test
    void shouldReturnEmptyOptionalFlightWhenDisruptionNotExists() {
        FlightDataSource flightDataSource = mock(FlightDataSource.class);
        when(flightDataSource.fetchFlightDisruption(any(), any())).thenReturn(Optional.empty());

        FlightDisruptionService service = new FlightDisruptionService(flightDataSource);

        Optional<FlightDisruptionResponse> result = service.findFlightDisruption("ab123", OffsetDateTime.now());

        assertTrue(result.isEmpty());
    }

    private Booking booking(String bookingRef, int passengers) {

        OffsetDateTime departure =
                OffsetDateTime.parse("2026-08-25T10:00:00+02:00");

        FlightSegment cancelledSegment = new FlightSegment(
                "EW100",
                "CGN",
                "IST",
                departure,
                departure.plusHours(3),
                FlightStatus.CANCELLED
        );

        return new Booking(
                bookingRef,
                passengers,
                new Journey(
                        "CGN",
                        "IST",
                        List.of(cancelledSegment)
                )
        );
    }

    @Test
    void shouldNotRecommendSameRouteWhenSeatsAreAlreadyConsumed() {

        FlightDataSource flightDataSource = mock(FlightDataSource.class);
        FlightDisruptionService service =
                new FlightDisruptionService(flightDataSource);

        OffsetDateTime departure =
                OffsetDateTime.parse("2026-08-25T10:00:00+02:00");

        Route route = new Route(
                "ALT-1",
                RouteType.FLIGHT,
                "CGN",
                "IST",
                departure,
                departure.plusHours(3),
                3,
                BigDecimal.valueOf(100),
                "Eurowings",
                null
        );

        FlightDisruptionResponse disruption =
                disruptionWithBookings(
                        booking("BOOKING-1", 2),
                        booking("BOOKING-2", 2)
                );

        FlightDisruptionDto result =
                service.findRecommendedRoutesForAllBookings(
                        disruption,
                        List.of(route)
                );

        FlightBookingDto firstBooking = result.flightBookings().stream()
                .filter(b -> b.id().equals("BOOKING-1"))
                .findFirst()
                .orElseThrow();

        FlightBookingDto secondBooking = result.flightBookings().stream()
                .filter(b -> b.id().equals("BOOKING-2"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, firstBooking.alternatives().size());

        assertTrue(
                secondBooking.alternatives().isEmpty(),
                "The second booking should not receive the route because " +
                        "only one seat remains after the first booking consumes 2 seats."
        );
    }

    private FlightDisruptionResponse disruptionWithBookings(Booking... bookings) {

        OffsetDateTime departure = OffsetDateTime.parse("2026-08-25T10:00:00+02:00");

        Disruption disruption = new Disruption(
                "EW100",
                "",
                "",
                departure,
                departure.plusHours(3),
                "CANCELLED",
                "Technical",
                OffsetDateTime.now(),
                10,
                20
        );

        return new FlightDisruptionResponse(
                disruption,
                List.of(bookings)
        );
    }

    @Test
    void shouldRecommendAlternativeIncludingWillBeMissedSegment() {

        FlightDisruptionService service =
                new FlightDisruptionService(mock(FlightDataSource.class));

        OffsetDateTime start =
                OffsetDateTime.parse("2026-08-25T08:00:00+02:00");

        OffsetDateTime deadline =
                OffsetDateTime.parse("2026-08-25T18:00:00+02:00");

        Booking booking = new Booking(
                "BOOKING-1",
                1,
                new Journey(
                        "CGN",
                        "IST",
                        List.of(
                                new FlightSegment(
                                        "EW1",
                                        "CGN",
                                        "FRA",
                                        start,
                                        start.plusHours(1),
                                        FlightStatus.CANCELLED
                                ),
                                new FlightSegment(
                                        "EW2",
                                        "FRA",
                                        "IST",
                                        deadline,
                                        deadline.plusHours(3),
                                        FlightStatus.WILL_BE_MISSED
                                )
                        )
                )
        );

        Route directAlternative = new Route(
                "ALT-DIRECT",
                RouteType.FLIGHT,
                "CGN",
                "IST",
                start.plusHours(2),
                deadline.minusMinutes(30),
                10,
                BigDecimal.valueOf(250),
                "Eurowings",
                null
        );

        FlightDisruptionDto result =
                service.findRecommendedRoutesForAllBookings(
                        disruptionWithBookings(booking),
                        List.of(directAlternative)
                );

        assertEquals(1, result.flightBookings().getFirst().alternatives().size());

        assertEquals(
                "IST",
                result.flightBookings().getFirst()
                        .alternatives()
                        .getFirst()
                        .to()
        );
    }

    private List<Route> readMockRoutes() throws IOException {
        var resource = new ClassPathResource("routes.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>(){});
        }
    }

    private FlightDisruptionResponse createMockFlightDisruption() {
        try {
            return readFlightDisruption();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FlightDisruptionResponse readFlightDisruption() throws IOException {
        var resource = new ClassPathResource("disruption.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, FlightDisruptionResponse.class);
        }
    }

}