package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart;

import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;

import java.math.BigDecimal;

public record CartItemDto(
    Long id,
    CartItemType type,

    Long merchandiseId,
    String name,
    BigDecimal unitPrice,
    Integer quantity,
    Integer remainingQuantity,
    Boolean hasImage,
    Long ticketId,
    Integer ticketCount,
    Long eventId,
    String eventTitle,
    Integer rowNumber,
    Integer seatNumber

) {
}

