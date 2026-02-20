package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
    Long id,
    List<CartItemDto> items,
    BigDecimal total
) {
}
