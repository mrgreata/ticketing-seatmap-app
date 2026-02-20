package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.CreditInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceMerchandiseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.InvoiceTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.SimpleTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceMerchandiseItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {


    @Mapping(target = "userId", source = "user.id")
    SimpleInvoiceDto toSimple(Invoice entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tickets", expression = "java(toInvoiceTicketDtos(entity.getTickets()))")
    @Mapping(target = "merchandiseItems", expression = "java(toMerchandiseItemDtos(entity.getMerchandiseItems()))")
    @Mapping(target = "invoiceDate", source = "invoiceDate")
    DetailedInvoiceDto toDetailed(Invoice entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "merchandiseItems", ignore = true)
    @Mapping(target = "invoiceCancellationDate", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "netTotal", ignore = true)
    @Mapping(target = "taxTotal", ignore = true)
    @Mapping(target = "grossTotal", ignore = true)
    @Mapping(target = "invoiceDate", ignore = true)
    @Mapping(target = "originalInvoiceNumber", ignore = true)
    @Mapping(target = "cancelledTickets", ignore = true)
    Invoice fromCreateDto(InvoiceCreateDto dto);

    default List<InvoiceTicketDto> toInvoiceTicketDtos(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }

        return tickets.stream()
            .map(ticket -> {

                String eventName = "Unbekanntes Event";
                String eventDate = null;

                if (ticket.getEvent() != null) {
                    eventName = ticket.getEvent().getTitle() != null
                        ? ticket.getEvent().getTitle() : eventName;

                    if (ticket.getEvent().getDateTime() != null) {
                        eventDate = ticket.getEvent().getDateTime().toString();
                    }
                }

                return new InvoiceTicketDto(ticket.getId(), eventName, eventDate);
            })
            .toList();
    }

    default List<InvoiceMerchandiseItemDto> toMerchandiseItemDtos(List<InvoiceMerchandiseItem> items) {
        return items.stream()
            .map(i -> new InvoiceMerchandiseItemDto(
                i.getMerchandise().getId(),
                i.getMerchandise().getName(),
                i.getMerchandise().getUnitPrice(),
                i.getQuantity(),
                i.getMerchandise().getRewardPointsPerUnit()
            ))
            .toList();
    }


    // Hilfsmethode: CancelledTicket -> SimpleTicketDto
    default SimpleTicketDto toSimpleTicketDto(CancelledTicket t) {
        // Hinweis: im Frontend erwarten wir nur invoiceId, locationId, id, eventId, sectorId, reservationId, seatId
        // Viele davon sind nicht verfügbar bei CancelledTicket, wir setzen default null oder 0
        return new SimpleTicketDto(
            t.getInvoice() != null ? t.getInvoice().getId().toString() : null, // invoiceId als String in DTO
            null,  // locationId
            t.getId(),
            null,  // eventId
            null,  // sectorId
            null,  // reservationId
            null   // seatId
        );
    }

    default List<SimpleTicketDto> toSimpleTicketDtos(List<Ticket> tickets) {
        if (tickets == null) {
            return Collections.emptyList();
        }
        return tickets.stream()
            .map(t -> new SimpleTicketDto(
                t.getInvoice() != null ? t.getInvoice().getId().toString() : null,
                t.getLocation() != null ? t.getLocation().getId() : null,
                t.getId(),
                t.getEvent() != null ? t.getEvent().getId() : null,
                t.getSector() != null ? t.getSector().getId() : null,
                t.getReservation() != null ? t.getReservation().getId() : null,
                t.getSeat() != null ? t.getSeat().getId() : null
            ))
            .toList();
    }

    // ---------- Optional: Mapping für CancelledTicketDto, falls benötigt ----------
    default CancelledTicketDto toCancelledTicketDto(CancelledTicket t) {
        List<String> seats;
        if (t.getSeat() != null && !t.getSeat().isEmpty()) {
            seats = Arrays.stream(t.getSeat().split(","))
                .map(String::trim)
                .toList();
        } else {
            seats = Collections.emptyList();
        }

        return new CancelledTicketDto(
            t.getId(),
            t.getEventName(),
            t.getEventDate(),
            t.getCancellationDate(),
            seats,
            t.getInvoice() != null ? t.getInvoice().getId() : null,
            false // selected default false
        );
    }


    default CreditInvoiceDto toCreditInvoiceDto(Invoice invoice) {
        List<InvoiceTicketDto> tickets = invoice.getCancelledTickets().stream()
            .map(t -> new InvoiceTicketDto(
                t.getId(),
                t.getEventName(),
                t.getEventDate() != null ? t.getEventDate().toString() : null // liefert ISO String
            ))
            .toList();

        return new CreditInvoiceDto(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getInvoiceCancellationDate(),
            tickets
        );
    }

}
