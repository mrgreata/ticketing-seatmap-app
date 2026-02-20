package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ArtistMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "locationCity", source = "location.city")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "minPrice", ignore = true)
    SimpleEventDto toSimple(Event event);

    @Mapping(target = "ticketCount", expression = "java(event.getTickets().size())")
    @Mapping(target = "minPrice", ignore = true)
    DetailedEventDto toDetailed(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "artists", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    Event fromCreateDto(EventCreateDto dto);

    @Mapping(target = "artists", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    Event fromUpdateDto(EventUpdateDto dto);
}