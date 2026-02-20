package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem;

import java.time.LocalDate;

public record DetailedNewsItemDto(
    Long id,
    String title,
    LocalDate publishedAt,
    String summary,
    String text
) {

}