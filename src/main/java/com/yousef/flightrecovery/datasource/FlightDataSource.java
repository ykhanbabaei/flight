package com.yousef.flightrecovery.datasource;

import com.yousef.flightrecovery.dto.disruption.FlightDisruptionResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class FlightDataSource {

    private final List<RoutesClient<? extends Record>> routesClients;

    final ObjectMapper objectMapper;

    public FlightDataSource(InternalRoutes internalRoutes, ExternalRoutes externalRoutes, TrainRoutes trainRoutes, ObjectMapper objectMapper) {
        this.routesClients = Arrays.asList(
                internalRoutes,
                externalRoutes,
                trainRoutes
        );
        this.objectMapper = objectMapper;
    }

    public Optional<FlightDisruptionResponse> fetchFlightDisruption(String flightNumber, OffsetDateTime scheduledDeparture) {
        try {
            var flightDisruption = readFlightDisruption();
            return Optional.of(flightDisruption);
            //TODO: In production check and find based on flightNumber and scheduledDeparture
            //if(flightNumber.equals(flightDisruption.disruption().flight()) && scheduledDeparture.toInstant().equals(flightDisruption.disruption().scheduledDeparture().toInstant())) {
            //    return Optional.of(flightDisruption);
            //} else {
            //    return Optional.empty();
            //}
        } catch (IOException e) {
            throw new DataSourceException("Error in fetching flight disruption data", e);
        }
    }

    public FlightDisruptionResponse readFlightDisruption() throws IOException {
        var resource = new ClassPathResource("data/disruption.json");
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, FlightDisruptionResponse.class);
        }
    }

    public List<RoutesClient<? extends Record>> getRoutesProviders() {
        return routesClients;
    }


}
