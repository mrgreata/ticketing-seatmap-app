package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase;

public record PaymentDetailDto(
    String cardNumber,
    String expiryMonthYear,
    String cvc,

    String paypalEmail

) {}
