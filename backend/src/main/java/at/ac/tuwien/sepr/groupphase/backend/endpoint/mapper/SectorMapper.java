package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.DetailedSectorDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SectorCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SimpleSectorDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SectorMapper {

    @Mapping(target = "locationId", ignore = true)
    SimpleSectorDto toSimple(Sector entity);

    @Mapping(target = "locationId", ignore = true)
    @Mapping(target = "seats", ignore = true)
    DetailedSectorDto toDetailed(Sector entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "seats", ignore = true)
    Sector fromCreateDto(SectorCreateDto dto);
}