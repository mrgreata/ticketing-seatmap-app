package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SectorCreateDto(
    @NotNull @Size(max = 255) String name,
    @NotNull Long locationId
) {

}