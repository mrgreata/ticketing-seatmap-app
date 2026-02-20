package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
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
 * REST endpoint for location management.
 */
@RestController
@RequestMapping("/api/v1/locations")
public class LocationEndpoint {

    private final LocationService locationService;
    private final EventService eventService;

    public LocationEndpoint(
        LocationService locationService,
        EventService eventService) {
        this.locationService = locationService;
        this.eventService = eventService;
    }

    /**
     * Get all locations.
     *
     * @return list of all locations
     */
    @PermitAll
    @GetMapping
    public List<SimpleLocationDto> findAll() {
        return locationService.findAllAsDto();
    }

    /**
     * Get location by ID with sectors.
     *
     * @param id the location ID
     * @return detailed location information including sectors
     */
    @PermitAll
    @GetMapping("/{id}")
    public DetailedLocationDto findById(@PathVariable(name = "id") Long id) {
        return locationService.findByIdAsDto(id);
    }

    /**
     * Create a new location (admin only).
     *
     * @param dto the location creation data
     * @return the created location
     */
    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleLocationDto create(@RequestBody LocationCreateDto dto) {
        return locationService.createFromDto(dto);
    }

    /**
     * Search locations by multiple optional criteria.
     *
     * @param name    location name (optional)
     * @param street  street name (optional)
     * @param city    city name (optional)
     * @param zipCode zip code (optional)
     * @return list of matching locations
     */
    @PermitAll
    @GetMapping("/search")
    public List<SimpleLocationDto> searchLocations(
        @RequestParam(required = false, name = "name") String name,
        @RequestParam(required = false, name = "street") String street,
        @RequestParam(required = false, name = "city") String city,
        @RequestParam(required = false, name = "zipCode") Integer zipCode
    ) {
        return locationService.searchLocationsAsDto(name, street, city, zipCode);
    }

    /**
     * Get all events at this location.
     *
     * @param id the location ID
     * @return list of events
     */
    @PermitAll
    @GetMapping("/{id}/events")
    public List<SimpleEventDto> getEventsByLocation(@PathVariable(name = "id") Long id) {
        return eventService.findByLocationIdAsDto(id);
    }
}