package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    SimpleLocationDto toSimple(Location entity);

    @Mapping(target = "sectors", ignore = true)
    DetailedLocationDto toDetailed(Location entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sectors", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "stageRowStart", ignore = true)
    @Mapping(target = "stageRowEnd", ignore = true)
    @Mapping(target = "stageColStart", ignore = true)
    @Mapping(target = "stageColEnd", ignore = true)
    @Mapping(target = "stageHeightPx", ignore = true)
    @Mapping(target = "stageWidthPx", ignore = true)
    @Mapping(target = "stagePosition", ignore = true)
    @Mapping(target = "stageLabel", ignore = true)
    @Mapping(target = "runwayWidthPx", ignore = true)
    @Mapping(target = "runwayLengthPx", ignore = true)
    @Mapping(target = "runwayOffsetPx", ignore = true)
    Location fromCreateDto(LocationCreateDto dto);
}