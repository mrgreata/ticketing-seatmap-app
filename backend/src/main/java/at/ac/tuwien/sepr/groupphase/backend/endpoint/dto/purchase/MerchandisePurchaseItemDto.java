package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MerchandisePurchaseItemDto(
    @NotNull Long merchandiseId,
    @NotNull @Min(1) Integer quantity
) {

}