package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.DetailedNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.SimpleNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsItemMapper {

    SimpleNewsItemDto toSimple(NewsItem entity);

    DetailedNewsItemDto toDetailed(NewsItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageData", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    @Mapping(target = "publishedAt", expression = "java(dto.publishedAt() != null ? dto.publishedAt() : java.time.LocalDate.now())")
    NewsItem fromCreateDto(NewsItemCreateDto dto);

    @Mapping(target = "imageData", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    @Mapping(target = "publishedAt", expression = "java(dto.publishedAt() != null ? dto.publishedAt() : java.time.LocalDate.now())")
    NewsItem fromUpdateDto(NewsItemUpdateDto dto);

    @Mapping(target = "imageData", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    NewsItem fromDetailedDto(DetailedNewsItemDto dto);
}