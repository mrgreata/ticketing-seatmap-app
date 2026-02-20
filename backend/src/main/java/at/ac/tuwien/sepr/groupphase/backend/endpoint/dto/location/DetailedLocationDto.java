package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SimpleSectorDto;
import java.util.List;

public record DetailedLocationDto(
    Long id,
    String name,
    int zipCode,
    String city,
    String street,
    String streetNumber,
    List<SimpleSectorDto> sectors,

    String stagePosition,
    String stageLabel,
    Integer stageHeightPx,
    Integer stageWidthPx,
    Integer stageRowStart,
    Integer stageRowEnd,
    Integer stageColStart,
    Integer stageColEnd,
    Integer runwayWidthPx,
    Integer runwayLengthPx,
    Integer runwayOffsetPx
) {

}