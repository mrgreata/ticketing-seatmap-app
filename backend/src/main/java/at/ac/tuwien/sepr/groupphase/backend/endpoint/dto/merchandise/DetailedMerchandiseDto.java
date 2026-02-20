package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise;

import java.math.BigDecimal;

public record DetailedMerchandiseDto(
    Long id,
    String description,
    String name,
    BigDecimal unitPrice,
    Integer rewardPointsPerUnit,
    Integer remainingQuantity,
    Boolean redeemableWithPoints,
    Boolean hasImage,
    Integer pointsPrice
) {

}