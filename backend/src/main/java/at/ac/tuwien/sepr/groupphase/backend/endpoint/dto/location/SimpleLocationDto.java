package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location;

public record SimpleLocationDto(
    Long id,
    String name,
    int zipCode,
    String city,
    String street,
    String streetNumber,

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