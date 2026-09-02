package com.yousef.flightrecovery.datasource;

import com.yousef.flightrecovery.dto.externalflight.ExternalFlightsResponse;
import com.yousef.flightrecovery.dto.externalflight.FlightOffer;
import com.yousef.flightrecovery.model.Route;
import com.yousef.flightrecovery.model.RouteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.yousef.flightrecovery.utils.MockBusySimulator.simulateDelay;

@Component
public class ExternalRoutes extends RoutesClient<ExternalFlightsResponse> {

    private static final Logger logger = LoggerFactory.getLogger(ExternalRoutes.class);


    public ExternalRoutes(ObjectMapper objectMapper) {
        super(objectMapper, ExternalFlightsResponse.class);
    }

    @Override
    public CompletableFuture<List<Route>> fetchRoutes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                simulateDelay(15);
                List<Route> routes = parseAndMap(readEntity("data/external-flights.json"));
                logger.info("From external data source {} routes fetched", routes.size());
                return routes;
            } catch (IOException e) {
                throw new DataSourceException("Error in fetching and mapping external routes", e);
            }
        }).exceptionally(this::handleException);

    }

    private List<Route> parseAndMap(ExternalFlightsResponse externalFlightsResponse) {
        var routes = new ArrayList<Route>();
        if (Objects.isNull(externalFlightsResponse)) {
            return routes;
        }

        List<FlightOffer> results = externalFlightsResponse.results();
        if (Objects.isNull(results)) {
            return routes;
        }

        for (FlightOffer flightOffer : results) {
            try {
                var id = flightOffer.offerId();
                var airlineName = flightOffer.airlineName();
                var fromAirport = flightOffer.departureAirport();
                var toAirport = flightOffer.arrivalAirport();
                var departureTime = flightOffer.departureTime();
                var arrivalTime = flightOffer.arrivalTime();
                var availableSeats = flightOffer.availability();

                var cents = flightOffer.priceCents();
                //TODO: cents should be converted based on currency, but for now we assume all prices are in euro cents.

                routes.add(new Route(
                        id,
                        RouteType.FLIGHT,
                        fromAirport,
                        toAirport,
                        departureTime.atOffset(ZoneOffset.UTC),
                        arrivalTime.atOffset(ZoneOffset.UTC),
                        availableSeats,
                        toBigDecimal(cents),
                        airlineName,
                        null
                ));

            } catch (Exception e) {
                logger.warn("Error in parsing route entry in external routes: ", e);
            }
        }

        return routes;
    }

    private static BigDecimal toBigDecimal(int cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

}
