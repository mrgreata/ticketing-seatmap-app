package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket;

public record InvoiceTicketDto(
    Long id,
    String eventTitle,
    String eventDate // ISO String, z.B. "2026-01-17T19:30:00"
) {

}