package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event;

import java.time.LocalDateTime;

public record SimpleEventDto(
    Long id,
    String title,
    String type,
    Integer durationMinutes,
    LocalDateTime dateTime,
    String locationName,
    String locationCity,
    Integer minPrice,
    String description
) {
}