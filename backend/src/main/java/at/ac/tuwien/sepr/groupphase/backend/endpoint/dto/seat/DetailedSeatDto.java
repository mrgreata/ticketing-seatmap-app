package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat;

public record DetailedSeatDto(
    Long id,
    int rowNumber,
    int seatNumber,
    Long sectorId
) {

}