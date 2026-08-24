package eurowings.assignment.datasource;

import eurowings.assignment.model.Route;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class RoutesClient<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> entityType;

    protected RoutesClient(ObjectMapper objectMapper, Class<T> entityType) {
        this.objectMapper = objectMapper;
        this.entityType = entityType;
    }

    protected T readEntity(String resourcePath) throws IOException {
        try (var inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {
            return objectMapper.readValue(inputStream, entityType);
        }
    }

    public abstract CompletableFuture<List<Route>> fetchRoutes();

}
