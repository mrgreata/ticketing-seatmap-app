package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriceCategoryCreateDto(
    @NotNull @Size(max = 255) String description,
    @NotNull int basePrice,
    @NotNull Long sectorId
) {

}