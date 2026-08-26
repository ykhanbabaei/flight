package eurowings.assignment.dto.bus;

import java.time.LocalDateTime;

public record BusConnection(String from, String to, LocalDateTime departure, LocalDateTime arrival){
}
