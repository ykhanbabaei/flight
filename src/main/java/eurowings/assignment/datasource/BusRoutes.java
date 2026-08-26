package eurowings.assignment.datasource;

import eurowings.assignment.dto.bus.BusResponse;
import eurowings.assignment.model.Route;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class BusRoutes extends RoutesClient<BusResponse>{

    protected BusRoutes(ObjectMapper objectMapper) {
        super(objectMapper, BusResponse.class);
    }

    @Override
    public CompletableFuture<List<Route>> fetchRoutes() {
        return CompletableFuture.<List<Route>>supplyAsync(() -> {
            throw new DataSourceException("Not implemented provider");
        }).exceptionally(this::handleException);
    }

}
