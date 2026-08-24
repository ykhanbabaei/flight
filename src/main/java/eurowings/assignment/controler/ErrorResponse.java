package eurowings.assignment.controler;

public record ErrorResponse(
        int status,
        String message
) {
}