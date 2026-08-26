package eurowings.assignment.dto.bus;

import java.util.List;

public record BusResponse(String id, List<BusConnection> connections){
}
