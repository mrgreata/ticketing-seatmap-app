package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event;

public record TopTenEventDto(
    Long eventId,
    String title,
    String type,
    Long ticketsSold
) {
}