package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record EventCreateDto(
    @NotNull @Size(max = 50) String title,
    @NotNull @Size(max = 100) String type,
    @NotNull @Min(1) @Max(9999) Integer durationMinutes,
    @Size(max = 800) String description,
    @NotNull LocalDateTime dateTime,
    @NotNull Long locationId,
    List<Long> artistIds
) {
}