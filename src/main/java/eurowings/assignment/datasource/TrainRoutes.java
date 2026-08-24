package eurowings.assignment.datasource;

import eurowings.assignment.dto.train.TrainConnection;
import eurowings.assignment.dto.train.TrainConnectionsDto;
import eurowings.assignment.model.Route;
import eurowings.assignment.model.RouteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static eurowings.assignment.utils.MockBusySimulator.simulateDelay;

@Component
public class TrainRoutes extends RoutesClient<TrainConnectionsDto> {

    private static final Logger logger = LoggerFactory.getLogger(TrainRoutes.class);

    public TrainRoutes(ObjectMapper objectMapper) {
        super(objectMapper, TrainConnectionsDto.class);
    }

    @Override
    public CompletableFuture<List<Route>> fetchRoutes() {
        //TODO: in production better to have dedicated customized thread pool executor
        return CompletableFuture.supplyAsync(() -> {
            try {
                simulateDelay(10);
                List<Route> routes = parseAndMap(readEntity("data/trains.json"));
                logger.info("From train data source {} routes fetched", routes.size());
                return routes;
            } catch (Exception e) {
                throw new DataSourceException("Error in fetching and mapping train routes", e);
            }
        });

    }

    private List<Route> parseAndMap(TrainConnectionsDto trainConnectionsDto) {
        var routes = new ArrayList<Route>();
        if (Objects.isNull(trainConnectionsDto)) {
            return routes;
        }
        var queries = trainConnectionsDto.queries();
        if (Objects.isNull(queries)) {
            return routes;
        }

        for (var query : queries) {
            var fromAirport = query.fromAirport();
            var toAirport = query.toAirport();

            var connections = query.connections();
            if (Objects.isNull(connections)) {
                continue;
            }

            for (TrainConnection conn : connections) {
                try {
                var id = conn.id();
                var trainOperator = conn.operator();
                var departureTime = conn.departure();
                var arrivalTime = conn.arrival();
                var availableSeats = conn.availableSeats();
                var price = conn.price();

                routes.add(new Route(
                        id,
                        RouteType.TRAIN,
                        fromAirport,
                        toAirport,
                        departureTime.atOffset(ZoneOffset.UTC),
                        arrivalTime.atOffset(ZoneOffset.UTC),
                        availableSeats,
                        toBigDecimal(price),
                        null,
                        trainOperator
                ));
                } catch (Exception e) {
                    logger.warn("Error in parsing route entry in train routes: ", e);
                }
            }
        }

        return routes;
    }

    private BigDecimal toBigDecimal(String price) {
        //TODO: cents should be converted based on currency, but for now we assume all prices are in euro cents.
        String[] parts = price.trim().split("\\s+");
        return new BigDecimal(parts[0]);
    }
}
