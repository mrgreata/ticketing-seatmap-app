package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase;

import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;

import java.util.List;

public record MerchandisePurchaseRequestDto(
    List<MerchandisePurchaseItemDto> items,
    PaymentMethod paymentMethod,
    PaymentDetailDto paymentDetail
) { }
