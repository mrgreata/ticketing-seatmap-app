package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;

import java.time.LocalDateTime;
import java.util.List;

public record DetailedEventDto(
    Long id,
    String title,
    String type,
    Integer durationMinutes,
    String description,
    LocalDateTime dateTime,
    SimpleLocationDto location,
    List<SimpleArtistDto> artists,
    int ticketCount,
    Integer minPrice
) {
}