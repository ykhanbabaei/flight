package eurowings.assignment.controler;

import eurowings.assignment.dto.disruption.FlightDisruptionResponse;
import eurowings.assignment.model.Route;
import eurowings.assignment.service.FlightDisruptionService;
import eurowings.assignment.service.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/v1")
public class FlightController {

    private static final Long SSE_EMITTER_TIMEOUT = 60000L;

    private final FlightDisruptionService flightDisruptionService;

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    public FlightController(FlightDisruptionService flightDisruptionService) {
        this.flightDisruptionService = flightDisruptionService;
    }

    @GetMapping(path = "/flights/stream-alternatives/{flightNumber}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter findBookingFlightsWithAlternativesAsync(@PathVariable @NotBlank String flightNumber, @RequestParam OffsetDateTime scheduledDeparture){
        Optional<FlightDisruptionResponse> flightDisruptionOpt = flightDisruptionService.findFlightDisruption(flightNumber, scheduledDeparture);
        if(flightDisruptionOpt.isEmpty()){
            throw new ResourceNotFoundException("Flight not found with flight number: " + flightNumber);
        }
        var emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        CompletableFuture<List<Route>> done = flightDisruptionService.findAlternativesAsync(accumulatedRoutes -> {
            try {
                var bookings = flightDisruptionService.findRecommendedRoutesForAllBookings(flightDisruptionOpt.get(), accumulatedRoutes);
                logger.info("Emitting alternative booking chunk for flight number {} and scheduled departure at {} ", flightNumber, scheduledDeparture);
                emitter.send(SseEmitter.event()
                        .name("alternatives-update")
                        .data(bookings));
            } catch (Exception e) {
                //log the error and continue sending alternative flights from other sources
                logger.warn("Error sending booking update event from a source for flight number {} and scheduled departure at {}.", flightNumber, scheduledDeparture, e);
            }
        });
        done.whenComplete((result, ex) -> {
            try {
                if (ex != null) {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                } else {
                    emitter.send(SseEmitter.event().name("complete").data("done"));
                }
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error in emitting data", e);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.error("Resource not found : {}", ex.getMessage(), ex);

        var error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

}
