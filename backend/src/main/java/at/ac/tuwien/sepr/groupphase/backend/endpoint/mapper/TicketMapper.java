package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.SimpleTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "seatId", source = "seat.id")
    SimpleTicketDto toSimple(Ticket entity);

    @Mapping(target = "eventName", source = "event.title")
    @Mapping(target = "rowNumber", source = "seat.rowNumber")
    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "locationCity", source = "location.city")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "reservationNumber", source = "reservation.reservationNumber")
    @Mapping(target = "eventDate", expression = "java(entity.getEvent().getDateTime().toLocalDate())")
    @Mapping(target = "entryTime", expression = "java(entity.getEvent().getDateTime().toLocalTime().toString())")
    @Mapping(target = "price", source = "grossPrice")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "seatId", source = "seat.id")
    DetailedTicketDto toDetailed(Ticket entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "sector", ignore = true)
    @Mapping(target = "seat", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "location", ignore = true)
    Ticket fromCreateDto(TicketCreateDto dto);


}