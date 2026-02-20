package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NewsItemCreateDto(
    @NotNull
    @Size(max = 50)
    String title,

    @NotNull
    @Size(max = 250)
    String summary,

    @NotNull
    @Size(max = 800)
    String text,

    LocalDate publishedAt
) {

}