package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation;

public record SimpleReservationDto(
    Long id,
    String reservationNumber,
    Long userId
) {

}