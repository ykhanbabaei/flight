package com.yousef.flightrecovery.service;

import com.yousef.flightrecovery.dto.AlternativeFlightDto;
import com.yousef.flightrecovery.dto.FlightDisruptionDto;
import com.yousef.flightrecovery.dto.disruption.Booking;
import com.yousef.flightrecovery.dto.disruption.FlightDisruptionResponse;
import com.yousef.flightrecovery.dto.disruption.FlightSegment;
import com.yousef.flightrecovery.dto.disruption.FlightStatus;
import com.yousef.flightrecovery.model.Route;
import com.yousef.flightrecovery.datasource.FlightDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.yousef.flightrecovery.utils.FlightEntityMapper;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class FlightDisruptionService {

    private static final Logger logger = LoggerFactory.getLogger(FlightDisruptionService.class);

    private final FlightDataSource flightDataSource;

    public FlightDisruptionService(FlightDataSource flightDataSource) {
        this.flightDataSource = flightDataSource;
    }

    public CompletableFuture<List<Route>> findAlternativesAsync(Consumer<List<Route>> onNextAccumulatedRoutesData) {
        try {
            var accumulatedRoutes = new ArrayList<Route>();
            var lock = new ReentrantLock();
            List<CompletableFuture<Void>> allData = flightDataSource.getRoutesProviders().stream()
                    .map(dataSource -> dataSource.fetchRoutes().thenAcceptAsync(newRoutes -> {
                        try {
                            lock.lock();
                            accumulatedRoutes.addAll(newRoutes);
                            onNextAccumulatedRoutesData.accept(accumulatedRoutes);
                        } finally {
                            lock.unlock();
                        }

                    })).toList();
            return CompletableFuture.allOf(allData.toArray(new CompletableFuture[0])).thenApply(v -> accumulatedRoutes);
        } catch (Exception e) {
            throw new InternalException("Error in fetching alternative flights", e);
        }
    }

    public FlightDisruptionDto findRecommendedRoutesForAllBookings(FlightDisruptionResponse flightDisruptionResponse, List<Route> routes) {
        try {
            return FlightEntityMapper.toFlightBookingDtoList(flightDisruptionResponse, findAndMapRecommendedRoutesForAllBookings(flightDisruptionResponse, routes));
        } catch (Exception e) {
            throw new InternalException("Error in finding and mapping recommended routes", e);
        }
    }

    public Optional<FlightDisruptionResponse> findFlightDisruption(String flightNumber, OffsetDateTime scheduledDeparture) {
        logger.info("Finding flight disruption with flight number {} and scheduled departure at {} ", flightNumber, scheduledDeparture);
        return flightDataSource.fetchFlightDisruption(flightNumber, scheduledDeparture);
    }


    private static Map<String, List<AlternativeFlightDto>> findAndMapRecommendedRoutesForAllBookings(FlightDisruptionResponse flightDisruptionResponse, List<Route> routes) {
        if(Objects.isNull(routes) || routes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<AlternativeFlightDto>> result = new LinkedHashMap<>();
        Map<String, Integer> remainingSeats = routes.stream().collect(Collectors.toMap(Route::id, Route::availableSeats));
        List<Booking> orderedBookings = sortBookings(flightDisruptionResponse.bookings(), routes, remainingSeats);
        for(Booking booking : orderedBookings) {
            List<AlternativeFlightDto> alternativeFlights = findAndMapRecommendedRoutes(booking, routes, remainingSeats);
            alternativeFlights.forEach(flight-> remainingSeats.merge(flight.id(), -booking.passengers(), Integer::sum));
            result.put(booking.bookingRef(), alternativeFlights);
        }
        return result;
    }

    private static List<Booking> sortBookings(List<Booking> bookings, List<Route> routes, Map<String, Integer> remainingSeats) {
        //TODO: Define a prioritization strategy for disrupted bookings.
        // Depending on airline policies and business rules, some bookings may receive higher priority.
        // For example, priority could be given to premium customers, business-class passengers,
        // or other high-value bookings.
        // Within the same priority level, different strategies could be applied, such as:
        // - Prioritizing simpler bookings with fewer passengers or journey legs.
        // - Applying a fairness policy where bookings are processed on a first-come, first-served basis.
        // The prioritization strategy should ideally be configurable to support different airline policies.
        return bookings;
    }

    /**
     * Finding the best alternative flight for given booking.
     * First try to find an alternative for canceled flight.
     * If not found based on NOT_DEPARTED and WILL_BE_MISSED segments
     * try to search different possibilities. If there is WILL_BE_MISSED segment also deadline constraint must be considered.
     * In general case we may have some NOT_DEPARTED segments then exactly one CANCELLED segment followed by zero or more WILL_BE_MISSED segments e.g.:
     * "segments": [
     * {
     * ...
     * "from": "A",
     * "to": "B",
     * "status": "NOT_DEPARTED",
     * ...
     * },
     * {
     * ...
     * "from": "B",
     * "to": "C",
     * "status": "CANCELLED",
     * ...
     * },
     * {
     * ...
     * "from": "C",
     * "to": "D",
     * "status": "WILL_BE_MISSED",
     * ...
     * },
     * {
     * ...
     * "from": "D",
     * "to": "E",
     * "status": "WILL_BE_MISSED",
     * ...
     * },
     * ]
     * There are many possibilities to check and the priority order depends on the policies. For example to keep the
     * costs minimum or give the customer convenient alternative journey by minimizing the legs.
     *
     * @param booking
     * @param routes
     * @param remainingSeats mutable map for keep tracking remained seats
     * @return
     */
    private static List<AlternativeFlightDto> findAndMapRecommendedRoutes(Booking booking, List<Route> routes, Map<String, Integer> remainingSeats) {
        Optional<FlightSegment> cancelledSegmentOpt = findCanceledSegment(booking);
        if(cancelledSegmentOpt.isEmpty()){
            logger.warn("Unexpected booking. Disruption booking flight must have exactly one cancelled segment. Booking id: '{}'", booking.bookingRef());
            return Collections.emptyList();
        }
        // First try to find alternative for canceled flight
        List<FlightSegment> missedSegmentList = findAllSegmentsWithStatus(booking, FlightStatus.WILL_BE_MISSED);
        OffsetDateTime deadline = OffsetDateTime.MAX;
        if(!missedSegmentList.isEmpty()){
            deadline = missedSegmentList.getFirst().departure();
        }
        FlightSegment cancelledSegment = cancelledSegmentOpt.get();
        List<Route> journeyRoutes = RouteFinder.findBestRoutesForJourney(cancelledSegment.from(), cancelledSegment.to(), deadline, booking.passengers(), routes, remainingSeats);
        if(!journeyRoutes.isEmpty()){
            logger.debug("For Cancelled flight, Alternative journey found. Legs: '{}', Booking id: '{}', From: '{}',  Departure:  '{}'", journeyRoutes.size(), booking.bookingRef(), cancelledSegment.from(), cancelledSegment.departure());
            return FlightEntityMapper.toAlternativeFlightDtoList(journeyRoutes);
        }

        // Then try to find the alternative by including 'will be missed' segments
        for(int index = 0; index < missedSegmentList.size(); index++){
            FlightSegment missedSegment = missedSegmentList.get(index);
            deadline = OffsetDateTime.MAX;
            if(missedSegmentList.size()-1 > index){//There are follow up missed segments
                deadline = missedSegmentList.get(index+1).departure();
            }
            journeyRoutes = RouteFinder.findBestRoutesForJourney(cancelledSegment.from(), missedSegment.to(), deadline, booking.passengers(), routes, remainingSeats);
            if(!journeyRoutes.isEmpty()){
                logger.debug("Alternative journey including will be missed segments found. Legs: '{}', Booking id: '{}', From: '{}',  To:  '{}'", journeyRoutes.size(), booking.bookingRef(), cancelledSegment.from(), missedSegment.to());
                return FlightEntityMapper.toAlternativeFlightDtoList(journeyRoutes);
            }
        }

        // Then try to find the alternative by including 'not departed' segments
        List<FlightSegment> noDepartedSegmentList = findAllSegmentsWithStatus(booking, FlightStatus.NOT_DEPARTED);
        deadline = OffsetDateTime.MAX;
        if(!missedSegmentList.isEmpty()){
            deadline = missedSegmentList.getFirst().departure();
        }
        for (int index = noDepartedSegmentList.size() - 1; index >= 0; index--) {
            FlightSegment notDepartedSegment = noDepartedSegmentList.get(index);
            journeyRoutes = RouteFinder.findBestRoutesForJourney(notDepartedSegment.from(), cancelledSegment.to(), deadline, booking.passengers(), routes, remainingSeats);
            if(!journeyRoutes.isEmpty()){
                logger.debug("Alternative journey including not departed legs found. Legs: '{}', Booking id: '{}', From: '{}',  To:  '{}'", journeyRoutes.size(), booking.bookingRef(), notDepartedSegment.from(), cancelledSegment.to());
                return FlightEntityMapper.toAlternativeFlightDtoList(journeyRoutes);
            }
        }

        // Then try to find the alternative for the whole journey
        journeyRoutes = RouteFinder.findBestRoutesForJourney(booking.journey().origin(), booking.journey().destination(), OffsetDateTime.MAX, booking.passengers(), routes, remainingSeats);
        if(!journeyRoutes.isEmpty()){
            logger.debug("Alternative journey found. Legs: '{}', Booking id: '{}', From: '{}',  To:  '{}'", journeyRoutes.size(), booking.bookingRef(), booking.journey().origin(), booking.journey().destination());
            return FlightEntityMapper.toAlternativeFlightDtoList(journeyRoutes);
        }

        // Could not find any!
        logger.info("No Alternative journey found. Booking id: '{}', From: '{}',  To:  '{}'", booking.bookingRef(), booking.journey().origin(), booking.journey().destination());
        return Collections.emptyList();
    }

    private static Optional<FlightSegment> findCanceledSegment(Booking booking) {
        return booking.journey().segments().stream().filter(segment -> segment.status() == FlightStatus.CANCELLED).findFirst();
    }

    private static List<FlightSegment> findAllSegmentsWithStatus(Booking booking, FlightStatus status) {
        return booking.journey().segments().stream().filter(segment -> segment.status() == status).toList();
    }


}
