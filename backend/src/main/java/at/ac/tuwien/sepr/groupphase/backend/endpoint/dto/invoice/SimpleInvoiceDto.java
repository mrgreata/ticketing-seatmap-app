package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice;

public record SimpleInvoiceDto(
    Long id,
    String invoiceNumber,
    Long userId
) {

}