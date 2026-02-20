package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart;

public record CartAddMerchandiseItemDto(
    Long merchandiseId,
    Integer quantity,
    Boolean redeemedWithPoints
) {
}
