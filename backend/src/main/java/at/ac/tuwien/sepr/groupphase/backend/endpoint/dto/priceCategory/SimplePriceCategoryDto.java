package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory;


public record SimplePriceCategoryDto(
    Long id,
    String description,
    int basePrice,
    Long sectorId
) {

}