package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice;

import java.math.BigDecimal;

public record InvoiceMerchandiseItemDto(
    Long merchandiseId,
    String name,
    BigDecimal unitPrice,
    Integer quantity,
    Integer rewardPointsPerUnit
) {
}
