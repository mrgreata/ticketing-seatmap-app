package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SimpleSeatDto;
import java.util.List;

public record DetailedSectorDto(
    Long id,
    String name,
    Long locationId,
    List<SimpleSeatDto> seats
) {

}