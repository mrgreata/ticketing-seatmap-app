package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    @Mapping(target = "memberIds", expression = "java(getMemberIds(entity))")
    SimpleArtistDto toSimple(Artist entity);

    DetailedArtistDto toDetailed(Artist entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "bandsWhereMember", ignore = true)
    Artist fromCreateDto(ArtistCreateDto dto);

    default List<Long> getMemberIds(Artist artist) {
        if (artist == null || artist.getMembers() == null) {
            return List.of();
        }
        return artist.getMembers().stream()
            .map(Artist::getId)
            .collect(Collectors.toList());
    }
}