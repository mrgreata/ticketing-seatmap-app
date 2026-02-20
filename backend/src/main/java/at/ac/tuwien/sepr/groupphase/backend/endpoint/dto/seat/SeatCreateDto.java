package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat;

import jakarta.validation.constraints.NotNull;

public record SeatCreateDto(
    @NotNull int rowNumber,
    @NotNull int seatNumber,
    @NotNull Long sectorId
) {

}