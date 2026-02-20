package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;

public record CartCheckoutRequestDto(
    PaymentMethod paymentMethod,
    PaymentDetailDto paymentDetail
) {
}
