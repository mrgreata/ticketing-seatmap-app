package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.InvoiceTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.SimpleTicketDto;

import java.time.LocalDate;
import java.util.List;

public record DetailedInvoiceDto(
    Long id,
    String invoiceNumber,
    LocalDate invoiceDate,
    Long userId,
    List<InvoiceTicketDto> tickets,
    List<InvoiceMerchandiseItemDto> merchandiseItems
) {

}