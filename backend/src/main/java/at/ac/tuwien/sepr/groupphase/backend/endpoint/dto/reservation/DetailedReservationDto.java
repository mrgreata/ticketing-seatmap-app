package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation;

import java.time.LocalDate;

public record DetailedReservationDto(
    Long reservationId,
    Long ticketId,
    String eventName,
    int rowNumber,
    int seatNumber,
    Long seatId,
    LocalDate eventDate,
    String entryTime,
    String reservationNumber,
    Double price,
    boolean selected
) {

}