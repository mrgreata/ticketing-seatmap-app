package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.TopTenEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing events.
 */
public interface EventService {

    /**
     * Find all events ordered by date (ascending).
     *
     * @return list of all events
     */
    List<Event> findAll();

    /**
     * Find event by ID.
     *
     * @param id the event ID
     * @return the event entity
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event not found
     */
    Event findById(Long id);

    /**
     * Create a new event.
     * Validates event data before creation.
     *
     * @param event the event to create
     * @return the created event
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if event data is invalid
     */
    Event create(Event event);

    /**
     * Update an existing event.
     * Validates event data before update.
     *
     * @param event the event to update
     * @return the updated event
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if event not found
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if event data is invalid
     */
    Event update(Event event);

    /**
     * Delete an event by ID.
     * Cannot delete events with existing tickets.
     *
     * @param id the event ID
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if event not found
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if event has tickets
     */
    void delete(Long id);

    /**
     * Search events by title (case-insensitive, partial match).
     *
     * @param title the title to search for
     * @return list of matching events
     */
    List<Event> searchByTitle(String title);

    /**
     * Search events by type (case-insensitive, exact match).
     *
     * @param type the event type
     * @return list of matching events
     */
    List<Event> searchByType(String type);

    /**
     * Search events by duration with tolerance.
     * Finds events within duration ± tolerance minutes.
     *
     * @param duration  the target duration in minutes
     * @param tolerance the tolerance range in minutes
     * @return list of matching events
     */
    List<Event> searchByDuration(int duration, int tolerance);

    /**
     * Search events by description (case-insensitive, partial match).
     *
     * @param description the description text to search for
     * @return list of matching events
     */
    List<Event> searchByDescription(String description);

    /**
     * Search events by a general search term.
     * Searches across title, description, and other fields.
     *
     * @param searchTerm the search term
     * @return list of matching events
     */
    List<Event> searchEvents(String searchTerm);

    /**
     * Find all events for a specific artist.
     *
     * @param artistId the artist ID
     * @return list of events featuring this artist
     */
    List<Event> findByArtistId(Long artistId);

    /**
     * Find all events at a specific location.
     *
     * @param locationId the location ID
     * @return list of events at this location
     */
    List<Event> findByLocationId(Long locationId);

    /**
     * Search events within a date range.
     *
     * @param start the start date/time
     * @param end   the end date/time
     * @return list of events within the range
     */
    List<Event> searchByDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Search events by multiple criteria (all optional).
     * Combines all provided criteria with AND logic.
     *
     * @param title       the title to search for (optional)
     * @param type        the event type (optional)
     * @param minDuration minimum duration in minutes (optional)
     * @param maxDuration maximum duration in minutes (optional)
     * @param startDate   earliest event date/time (optional)
     * @param endDate     latest event date/time (optional)
     * @param locationId  the location ID (optional)
     * @return list of matching events
     */
    List<Event> searchByCriteria(
        String title,
        String type,
        Integer minDuration,
        Integer maxDuration,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long locationId
    );

    /**
     * Search events by time range and price range.
     * Finds events with at least one ticket in the price range.
     *
     * @param startDate  earliest event date/time (optional)
     * @param endDate    latest event date/time (optional)
     * @param minPrice   minimum ticket price in cents (optional)
     * @param maxPrice   maximum ticket price in cents (optional)
     * @param locationId the location ID (optional)
     * @return list of matching events
     */
    List<Event> searchByTimeAndPrice(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer minPrice,
        Integer maxPrice,
        Long locationId
    );

    /**
     * Validate event data.
     * Checks date is in future, location exists, and title is present.
     *
     * @param event the event to validate
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if validation fails
     */
    void validateEventData(Event event);

    /**
     * Find top 10 events by ticket sales for a specific month and year.
     * Optionally filter by event type.
     *
     * @param month the month (1-12)
     * @param year  the year
     * @param type  the event type filter (optional)
     * @return list of top 10 events by ticket count
     */
    List<TopTenEventDto> findTopTenByTicketSales(int month, int year, String type);

    /**
     * Get seatmap for an event including seat status (free/reserved/sold).
     *
     * @param eventId the event ID
     * @return the seatmap with all seats and their status
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event not found
     */
    SeatmapDto getSeatmap(Long eventId);

    /**
     * Find minimum ticket price for an event.
     * Returns null if no tickets/prices exist.
     *
     * @param eventId the event ID
     * @return the minimum price in cents, or null if no price found
     */
    Integer findMinPriceForEvent(Long eventId);

    /**
     * Upload an image for an event.
     * Validates file size (max 5MB) and type (JPEG/PNG/WebP).
     *
     * @param eventId the event ID
     * @param image   the image file
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if event not found
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if image is invalid
     */
    void uploadImage(Long eventId, MultipartFile image);

    /**
     * Get image data for an event.
     *
     * @param eventId the event ID
     * @return the image bytes
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event or image not found
     */
    byte[] getImage(Long eventId);

    /**
     * Get image content type for an event.
     *
     * @param eventId the event ID
     * @return the MIME type (e.g., "image/jpeg")
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event or image not found
     */
    String getImageContentType(Long eventId);

    /**
     * Find all events ordered by date with pagination.
     *
     * @param pageable pagination information
     * @return page of events as DTOs
     */
    Page<SimpleEventDto> findAllAsDto(Pageable pageable);

    /**
     * Get event by ID with enriched price information as detailed DTO.
     * Includes location, artists, and minimum ticket price.
     *
     * @param id the event ID
     * @return the event as detailed DTO
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event not found
     */
    DetailedEventDto findByIdAsDto(Long id);

    /**
     * Create event from DTO including location and artists resolution.
     * Resolves location and artist IDs to actual entities before creation.
     *
     * @param dto the event creation data
     * @return the created event as DTO with enriched price
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if data is invalid
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if location or artist not found
     */
    SimpleEventDto createFromDto(EventCreateDto dto);

    /**
     * Update event from DTO including location and artists resolution.
     * Resolves location and artist IDs to actual entities before update.
     *
     * @param id  the event ID (must match DTO ID)
     * @param dto the event update data
     * @return the updated event as detailed DTO
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if data is invalid or IDs don't match
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if event, location, or artist not found
     */
    DetailedEventDto updateFromDto(Long id, EventUpdateDto dto);

    /**
     * Advanced event search with pagination.
     *
     * @param title      event title (optional)
     * @param type       event type (optional)
     * @param duration   target duration with ±30min tolerance (optional)
     * @param dateFrom   earliest event date (optional)
     * @param dateTo     latest event date (optional)
     * @param locationId location ID (optional)
     * @param priceMin   minimum ticket price in cents (optional)
     * @param priceMax   maximum ticket price in cents (optional)
     * @param pageable   pagination information
     * @return page of matching events
     */
    Page<SimpleEventDto> searchEventsAsDto(
        String title,
        String type,
        Integer duration,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        Long locationId,
        Integer priceMin,
        Integer priceMax,
        Pageable pageable
    );

    /**
     * Find events by location ID and return as DTOs with enriched prices.
     *
     * @param locationId the location ID
     * @return list of events at this location as DTOs
     */
    List<SimpleEventDto> findByLocationIdAsDto(Long locationId);

    /**
     * Find events by artist ID and return as DTOs with enriched prices.
     *
     * @param artistId the artist ID
     * @return list of events featuring this artist as DTOs
     */
    List<SimpleEventDto> findByArtistIdAsDto(Long artistId);

    /**
     * Delete the image of an event.
     *
     * @param eventId the event ID
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if event not found
     */
    void deleteImage(Long eventId);
}