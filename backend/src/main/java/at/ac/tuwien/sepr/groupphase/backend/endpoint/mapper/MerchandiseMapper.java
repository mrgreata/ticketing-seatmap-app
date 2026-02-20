package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.MerchandiseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.SimpleMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.merchandise.DetailedMerchandiseDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MerchandiseMapper {

    @Mapping(target = "hasImage", expression = "java(entity.getImage() != null && entity.getImage().length > 0)")
    SimpleMerchandiseDto toSimple(Merchandise entity);

    @Mapping(target = "hasImage", expression = "java(entity.getImage() != null && entity.getImage().length > 0)")
    DetailedMerchandiseDto toDetailed(Merchandise entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    Merchandise fromCreateDto(MerchandiseCreateDto dto);
}