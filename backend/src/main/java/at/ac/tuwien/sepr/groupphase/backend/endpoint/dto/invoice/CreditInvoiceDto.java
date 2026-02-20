package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.InvoiceTicketDto;

import java.time.LocalDate;
import java.util.List;

public record CreditInvoiceDto(
    Long id,
    String invoiceNumber,
    LocalDate invoiceCancellationDate,
    List<InvoiceTicketDto> tickets
) {}