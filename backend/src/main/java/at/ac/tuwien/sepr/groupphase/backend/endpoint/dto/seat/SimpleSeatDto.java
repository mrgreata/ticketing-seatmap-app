package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat;

public record SimpleSeatDto(
    Long id,
    int rowNumber,
    int seatNumber,
    Long sectorId
) {

}