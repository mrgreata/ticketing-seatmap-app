package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DetailedTicketDto(
    Long id,
    String eventName,
    int rowNumber,
    int seatNumber,
    Long seatId,
    LocalDate eventDate,
    String entryTime,
    String invoiceNumber,
    String reservationNumber,
    Double price,
    String locationCity,
    boolean selected,
    Long eventId,
    Long sectorId,
    Long reservationId,
    Long invoiceId
) {

}