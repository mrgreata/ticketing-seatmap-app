package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.ReservationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReservationMapper {

    // --- Reservation -> DTO ---

    @Mapping(target = "userId", source = "user.id")
    SimpleReservationDto toSimple(Reservation entity);

    /**
     * DetailedReservationDto is ticket-centric in this project.
     * Use toTicketDetailed(ticket, reservation) instead.
     */
    default DetailedReservationDto toDetailed(Reservation entity) {
        throw new UnsupportedOperationException("Use toTicketDetailed(ticket, reservation) for DetailedReservationDto");
    }

    // --- Ticket + Reservation -> DetailedReservationDto (ticket-centric) ---

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "eventName", source = "ticket.event.title")
    @Mapping(target = "rowNumber", source = "ticket.seat.rowNumber")
    @Mapping(target = "seatNumber", source = "ticket.seat.seatNumber")
    @Mapping(target = "seatId", source = "ticket.seat.id")
    @Mapping(target = "eventDate", expression = "java(ticket.getEvent().getDateTime().toLocalDate())")
    @Mapping(target = "entryTime", expression = "java(ticket.getEvent().getDateTime().toLocalTime().toString())")
    @Mapping(target = "reservationNumber", source = "reservation.reservationNumber")
    @Mapping(target = "price", expression = "java(ticket.getGrossPrice())")
    @Mapping(target = "selected", constant = "true")
    DetailedReservationDto toTicketDetailed(Ticket ticket, Reservation reservation);

    // --- Create DTO -> Entity ---

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "seats", ignore = true)
    @Mapping(target = "reservationNumber", ignore = true)
    Reservation fromCreateDto(ReservationCreateDto dto);
}
