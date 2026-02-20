package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart;

public record CartCheckoutResultDto(
    Long merchandiseInvoiceId,
    Long ticketInvoiceId
) {
}
