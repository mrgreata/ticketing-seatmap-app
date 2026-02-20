package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory;

public record DetailedPriceCategoryDto(
    Long id,
    String description,
    int basePrice,
    Long sectorId
) {

}