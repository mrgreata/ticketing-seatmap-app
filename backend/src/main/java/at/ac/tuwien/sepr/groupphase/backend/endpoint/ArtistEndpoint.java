package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for artist management.
 */
@RestController
@RequestMapping("/api/v1/artists")
public class ArtistEndpoint {

    private final ArtistService artistService;
    private final EventService eventService;

    public ArtistEndpoint(
        ArtistService artistService,
        EventService eventService) {
        this.artistService = artistService;
        this.eventService = eventService;
    }

    /**
     * Get all artists.
     *
     * @return list of all artists
     */
    @PermitAll
    @GetMapping
    public List<SimpleArtistDto> findAll() {
        return artistService.findAllAsDto();
    }

    /**
     * Get artist by ID with details.
     *
     * @param id the artist ID
     * @return detailed artist information
     */
    @PermitAll
    @GetMapping("/{id}")
    public DetailedArtistDto findById(@PathVariable(name = "id") Long id) {
        return artistService.findByIdAsDto(id);
    }

    /**
     * Create a new artist (admin only).
     * If artist is a band, member IDs must be provided.
     *
     * @param dto the artist creation data
     * @return the created artist
     */
    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleArtistDto create(@RequestBody ArtistCreateDto dto) {
        return artistService.createFromDto(dto);
    }

    /**
     * Search artists by name.
     *
     * @param name         the search term
     * @param includeBands if true, also returns bands where artist is a member
     * @return list of matching artists
     */
    @PermitAll
    @GetMapping("/search")
    public List<SimpleArtistDto> searchArtists(
        @RequestParam(name = "name") String name,
        @RequestParam(name = "includeBands", defaultValue = "true") boolean includeBands) {
        return artistService.searchArtistsAsDto(name, includeBands);
    }

    /**
     * Get all events featuring this artist.
     *
     * @param id the artist ID
     * @return list of events
     */
    @PermitAll
    @GetMapping("/{id}/events")
    public List<SimpleEventDto> getEventsByArtist(@PathVariable(name = "id") Long id) {
        return eventService.findByArtistIdAsDto(id);
    }
}