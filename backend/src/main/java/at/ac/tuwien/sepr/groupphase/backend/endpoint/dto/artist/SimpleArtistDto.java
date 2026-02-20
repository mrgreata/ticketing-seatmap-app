package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist;

import java.util.List;

public record SimpleArtistDto(
    Long id,
    String name,
    Boolean isBand,
    List<Long> memberIds
) {

}