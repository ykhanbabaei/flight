package com.yousef.flightrecovery.datasource;

import com.yousef.flightrecovery.dto.internalflight.FlightSearch;
import com.yousef.flightrecovery.dto.internalflight.InternalFlightOffer;
import com.yousef.flightrecovery.dto.internalflight.InternalFlightsResponse;
import com.yousef.flightrecovery.model.Route;
import com.yousef.flightrecovery.model.RouteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.yousef.flightrecovery.utils.MockBusySimulator.simulateDelay;

@Component
public class InternalRoutes extends RoutesClient<InternalFlightsResponse> {

    private static final Logger logger = LoggerFactory.getLogger(InternalRoutes.class);

    public InternalRoutes(ObjectMapper objectMapper) {
        super(objectMapper, InternalFlightsResponse.class);
    }

    @Override
    public CompletableFuture<List<Route>> fetchRoutes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                simulateDelay(4);
                List<Route> routes = parseAndMap(readEntity("data/internal-flights.json"));
                logger.info("From internal data source {} routes fetched", routes.size());
                return routes;
            } catch (Exception e) {
                throw new DataSourceException("Error in fetching and mapping internal routes", e);
            }
        }).exceptionally(this::handleException);

    }

    private List<Route> parseAndMap(InternalFlightsResponse internalFlightsResponse) {
        var routes = new ArrayList<Route>();
        if (Objects.isNull(internalFlightsResponse)) {
            return routes;
        }

        List<FlightSearch> searches = internalFlightsResponse.searches();
        if (Objects.isNull(searches)) {
            return routes;
        }

        for (FlightSearch search : searches) {
            try {

                List<InternalFlightOffer> offers = search.offers();
                if (Objects.isNull(offers)) {
                    continue;
                }

                for (InternalFlightOffer offer : offers) {
                    String airlineName = offer.carrier();
                    var id = offer.offerId();
                    var fromAirport = offer.departureAirport();
                    var toAirport = offer.arrivalAirport();
                    var departureTime = offer.departure();
                    var arrivalTime = offer.arrival();
                    var availableSeats = offer.availableSeats();

                    BigDecimal price = null;
                    var priceNode = offer.pricePerSeat();
                    if (Objects.nonNull(priceNode)) {
                        price = priceNode.amount();
                        //TODO: cents should be converted based on currency, but for now we assume all prices are in euro cents.
                    }

                    routes.add(new Route(
                            id,
                            RouteType.FLIGHT,
                            fromAirport,
                            toAirport,
                            departureTime,
                            arrivalTime,
                            availableSeats,
                            price,
                            airlineName,
                            null
                    ));
                }
            } catch (Exception e) {
                logger.warn("Error in parsing route entry in internal routes: ", e);
            }


        }

        return routes;
    }
}
