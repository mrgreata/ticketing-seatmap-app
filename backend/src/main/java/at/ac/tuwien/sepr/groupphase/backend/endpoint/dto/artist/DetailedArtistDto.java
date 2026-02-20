package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist;

import java.util.List;

public record DetailedArtistDto(
    Long id,
    String name,
    Boolean isBand,
    List<SimpleArtistDto> members,
    List<SimpleArtistDto> bandsWhereMember
) {

}