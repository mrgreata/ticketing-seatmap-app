package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MerchandiseCreateDto(
    @NotNull @Size(max = 255) String description,
    @NotNull @Size(max = 255) String name,
    @NotNull BigDecimal unitPrice,
    @NotNull Integer rewardPointsPerUnit,
    @NotNull Integer remainingQuantity,
    @NotNull Boolean redeemableWithPoints,
    Integer pointsPrice
) {

}