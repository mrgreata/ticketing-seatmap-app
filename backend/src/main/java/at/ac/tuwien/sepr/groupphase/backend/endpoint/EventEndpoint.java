package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.TopTenEventDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST endpoint for event management.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventEndpoint {

    private final EventService eventService;

    public EventEndpoint(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Get all events ordered by date with pagination support.
     *
     * @param page the page number (default: 0)
     * @param size the page size (default: 12)
     * @return page of events with minimum prices
     */
    @PermitAll
    @GetMapping
    public Page<SimpleEventDto> findAll(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return eventService.findAllAsDto(pageable);
    }

    /**
     * Get event by ID with full details.
     *
     * @param id the event ID
     * @return detailed event information
     */
    @PermitAll
    @GetMapping("/{id}")
    public DetailedEventDto findById(@PathVariable(name = "id") Long id) {
        return eventService.findByIdAsDto(id);
    }

    /**
     * Create a new event (admin only).
     *
     * @param dto the event creation data
     * @return the created event
     */
    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleEventDto create(@RequestBody EventCreateDto dto) {
        return eventService.createFromDto(dto);
    }

    /**
     * Update an existing event (admin only).
     *
     * @param id  the event ID
     * @param dto the event update data
     * @return the updated event
     */
    @Secured("ROLE_ADMIN")
    @PutMapping("/{id}")
    public DetailedEventDto update(
        @PathVariable("id") Long id,
        @RequestBody EventUpdateDto dto) {
        return eventService.updateFromDto(id, dto);
    }

    /**
     * Delete an event (admin only).
     * Cannot delete events with existing tickets.
     *
     * @param id the event ID
     */
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        eventService.delete(id);
    }

    /**
     * Get seatmap for an event showing seat status (free/reserved/sold).
     *
     * @param eventId the event ID
     * @return seatmap with all seats and their status
     */
    @PermitAll
    @GetMapping("/{eventId}/seatmap")
    public SeatmapDto getSeatmap(@PathVariable("eventId") Long eventId) {
        return eventService.getSeatmap(eventId);
    }

    /**
     * Advanced event search with multiple optional criteria and pagination.
     * Duration includes ±30min tolerance. Supports price filtering.
     *
     * @param title      event title (optional)
     * @param type       event type (optional)
     * @param duration   target duration with ±30min tolerance (optional)
     * @param dateFrom   earliest event date (optional)
     * @param dateTo     latest event date (optional)
     * @param locationId location ID (optional)
     * @param priceMin   minimum ticket price in cents (optional)
     * @param priceMax   maximum ticket price in cents (optional)
     * @param page       the page number (default: 0)
     * @param size       the page size (default: 12)
     * @return page of matching events
     */
    @PermitAll
    @GetMapping("/search")
    public Page<SimpleEventDto> searchEvents(
        @RequestParam(required = false, name = "title") String title,
        @RequestParam(required = false, name = "type") String type,
        @RequestParam(required = false, name = "duration") Integer duration,
        @RequestParam(required = false, name = "dateFrom")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
        @RequestParam(required = false, name = "dateTo")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
        @RequestParam(required = false, name = "locationId") Long locationId,
        @RequestParam(required = false, name = "priceMin") Integer priceMin,
        @RequestParam(required = false, name = "priceMax") Integer priceMax,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return eventService.searchEventsAsDto(
            title, type, duration, dateFrom, dateTo, locationId, priceMin, priceMax, pageable);
    }

    /**
     * Get top 10 events by ticket sales for a specific month and year.
     *
     * @param month the month (1-12)
     * @param year  the year
     * @param type  event type filter (optional)
     * @return list of top 10 events by sold tickets
     */
    @PermitAll
    @GetMapping("/top-ten")
    public List<TopTenEventDto> getTopTen(
        @RequestParam(name = "month") int month,
        @RequestParam(name = "year") int year,
        @RequestParam(required = false, name = "type") String type
    ) {
        return eventService.findTopTenByTicketSales(month, year, type);
    }

    /**
     * Upload an image for an event (admin only).
     * Max size: 5MB. Allowed types: JPEG, PNG, WebP.
     *
     * @param id    the event ID
     * @param image the image file
     * @return empty response with 200 OK
     */
    @Secured("ROLE_ADMIN")
    @PostMapping(
        value = "/{id}/image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadImage(
        @PathVariable("id") Long id,
        @RequestParam("image") MultipartFile image
    ) {
        eventService.uploadImage(id, image);
        return ResponseEntity.ok().build();
    }

    /**
     * Get image for an event.
     *
     * @param id the event ID
     * @return the image file
     */
    @PermitAll
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        try {
            byte[] data = eventService.getImage(id);
            String contentType = eventService.getImageContentType(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(data.length);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (NotFoundException e) {
            if (e.getMessage().contains("kein Bild")) {
                return ResponseEntity.noContent().build();
            }
            throw e;
        }
    }

    /**
     * Delete image for an event (admin only).
     *
     * @param id the event ID
     */
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable("id") Long id) {
        eventService.deleteImage(id);
    }
}