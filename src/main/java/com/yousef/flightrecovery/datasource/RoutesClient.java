package com.yousef.flightrecovery.datasource;

import com.yousef.flightrecovery.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class RoutesClient<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> entityType;
    private static final Logger logger = LoggerFactory.getLogger(RoutesClient.class);

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

    protected List<Route> handleException(Throwable ex) {
        logger.error("Error in fetching data source ", ex);
        return Collections.emptyList();
    }

    public abstract CompletableFuture<List<Route>> fetchRoutes();

}
