package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.PriceCategoryCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.SimplePriceCategoryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.DetailedPriceCategoryDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceCategoryMapper {

    @Mapping(target = "sectorId", ignore = true)
    SimplePriceCategoryDto toSimple(PriceCategory entity);

    @Mapping(target = "sectorId", ignore = true)
    DetailedPriceCategoryDto toDetailed(PriceCategory entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sector", ignore = true)
    PriceCategory fromCreateDto(PriceCategoryCreateDto dto);
}