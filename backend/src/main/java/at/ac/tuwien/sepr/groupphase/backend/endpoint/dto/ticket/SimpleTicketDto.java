package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket;

public record SimpleTicketDto(
    String invoiceId,
    Long locationId,
    Long id,
    Long eventId,
    Long sectorId,
    Long reservationId,
    Long seatId
) {

}