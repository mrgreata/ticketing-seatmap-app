package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket;

import jakarta.validation.constraints.NotNull;

public record TicketCreateDto(
    @NotNull Long eventId,
    @NotNull Long seatId
) {

}