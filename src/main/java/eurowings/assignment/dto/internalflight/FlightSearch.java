package eurowings.assignment.dto.internalflight;

import java.util.List;

public record FlightSearch(
        String searchId,
        FlightSearchQuery query,
        List<InternalFlightOffer> offers
) {
}