package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SimpleSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.DetailedSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatmapSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "sectorId", ignore = true)
    SimpleSeatDto toSimple(Seat entity);

    @Mapping(target = "sectorId", ignore = true)
    DetailedSeatDto toDetailed(Seat entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sector", ignore = true)
    @Mapping(target = "priceCategory", ignore = true)
    Seat fromCreateDto(SeatCreateDto dto);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "priceCategory", ignore = true)
    @Mapping(target = "sectorId", source = "sector.id")
    SeatmapSeatDto seatToSeatmapSeatDto(Seat seat);

    default SeatmapSeatDto seatToSeatmapSeatDto(Seat seat, SeatStatus status) {
        SeatmapSeatDto dto = seatToSeatmapSeatDto(seat);
        dto.setStatus(status);
        if (seat.getPriceCategory() != null) {
            dto.setPriceCategory(seat.getPriceCategory().getDescription());
        } else {
            dto.setPriceCategory("middle");
        }
        return dto;
    }
}
