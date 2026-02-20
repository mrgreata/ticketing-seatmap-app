package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link Event} entities.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Find events by title containing search term (case-insensitive).
     *
     * @param title the search term
     * @return list of matching events
     */
    List<Event> findByTitleContainingIgnoreCase(String title);

    /**
     * Find events by exact type (case-insensitive).
     *
     * @param type the event type (e.g., "CONCERT", "OPERA")
     * @return list of matching events
     */
    List<Event> findByTypeIgnoreCase(String type);

    /**
     * Find events with duration in the specified range.
     *
     * @param minDuration minimum duration in minutes
     * @param maxDuration maximum duration in minutes
     * @return list of events within the duration range
     */
    @Query("SELECT e FROM Event e WHERE e.durationMinutes BETWEEN :minDuration AND :maxDuration")
    List<Event> findByDurationBetween(
        @Param("minDuration") int minDuration,
        @Param("maxDuration") int maxDuration
    );

    /**
     * Find events by description containing search term (case-insensitive).
     *
     * @param description the search term
     * @return list of matching events
     */
    List<Event> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Search events by a general search term across multiple fields.
     * Searches in title, type, and description.
     *
     * @param searchTerm the search term
     * @return list of matching events
     */
    @Query("SELECT DISTINCT e FROM Event e WHERE "
        + "LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
        + "LOWER(e.type) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
        + "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Event> searchEvents(@Param("searchTerm") String searchTerm);

    /**
     * Find all events featuring a specific artist.
     *
     * @param artistId the artist ID
     * @return list of events with this artist
     */
    @Query("SELECT e FROM Event e JOIN e.artists a WHERE a.id = :artistId")
    List<Event> findByArtistId(@Param("artistId") Long artistId);

    /**
     * Find all events at a specific location.
     *
     * @param locationId the location ID
     * @return list of events at this location
     */
    List<Event> findByLocationId(Long locationId);

    /**
     * Find all events ordered by date ascending (soonest first).
     *
     * @return list of all events ordered by date/time
     */
    List<Event> findAllByOrderByDateTimeAsc();

    /**
     * Find events within a specific date/time range.
     *
     * @param start the start date/time
     * @param end   the end date/time
     * @return list of events within the range
     */
    List<Event> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Advanced search with multiple optional criteria.
     * All criteria are combined with AND logic. Null parameters are ignored.
     *
     * @param title       the title search term (optional, case-insensitive partial match)
     * @param type        the event type (optional, case-insensitive exact match)
     * @param minDuration minimum duration in minutes (optional)
     * @param maxDuration maximum duration in minutes (optional)
     * @param startDate   earliest event date/time (optional)
     * @param endDate     latest event date/time (optional)
     * @param locationId  the location ID (optional)
     * @return list of matching events
     */
    @Query("SELECT DISTINCT e FROM Event e "
        + "WHERE (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%'))) "
        + "AND (:type IS NULL OR LOWER(e.type) = LOWER(:type)) "
        + "AND (:minDuration IS NULL OR e.durationMinutes >= :minDuration) "
        + "AND (:maxDuration IS NULL OR e.durationMinutes <= :maxDuration) "
        + "AND (:startDate IS NULL OR e.dateTime >= :startDate) "
        + "AND (:endDate IS NULL OR e.dateTime <= :endDate) "
        + "AND (:locationId IS NULL OR e.location.id = :locationId)")
    List<Event> searchEventsByCriteria(
        @Param("title") String title,
        @Param("type") String type,
        @Param("minDuration") Integer minDuration,
        @Param("maxDuration") Integer maxDuration,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("locationId") Long locationId
    );

    /**
     * Search events with price filter via Location → Sector → PriceCategory chain.
     * Finds events that have at least one seat in the specified price range.
     *
     * @param startDate  earliest event date/time (optional)
     * @param endDate    latest event date/time (optional)
     * @param minPrice   minimum base price in cents (optional)
     * @param maxPrice   maximum base price in cents (optional)
     * @param locationId the location ID (optional)
     * @return list of events matching the criteria
     */
    @Query("SELECT DISTINCT e FROM Event e "
        + "JOIN e.location l "
        + "JOIN l.sectors s "
        + "JOIN PriceCategory pc ON pc.sector = s "
        + "WHERE (:startDate IS NULL OR e.dateTime >= :startDate) "
        + "AND (:endDate IS NULL OR e.dateTime <= :endDate) "
        + "AND (:minPrice IS NULL OR pc.basePrice >= :minPrice) "
        + "AND (:maxPrice IS NULL OR pc.basePrice <= :maxPrice) "
        + "AND (:locationId IS NULL OR e.location.id = :locationId)")
    List<Event> searchEventsByTimeAndPrice(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("minPrice") Integer minPrice,
        @Param("maxPrice") Integer maxPrice,
        @Param("locationId") Long locationId
    );

    /**
     * Find top 10 events by ticket sales for a specific month and year.
     * Only counts sold tickets (tickets with an invoice).
     * Optionally filter by event type.
     *
     * @param month the month (1-12)
     * @param year  the year (e.g., 2026)
     * @param type  the event type filter (optional)
     * @return list of Object arrays containing [eventId, title, type, ticketCount]
     */
    @Query("SELECT e.id, e.title, e.type, COUNT(t.id) as ticketCount "
        + "FROM Event e "
        + "JOIN e.tickets t "
        + "WHERE t.invoice IS NOT NULL "
        + "AND t.invoice.invoiceCancellationDate IS NULL "
        + "AND YEAR(e.dateTime) = :year "
        + "AND MONTH(e.dateTime) = :month "
        + "AND (:type IS NULL OR LOWER(e.type) = LOWER(:type)) "
        + "GROUP BY e.id, e.title, e.type "
        + "ORDER BY COUNT(t.id) DESC "
        + "LIMIT 10")
    List<Object[]> findTopTenEventsByTicketSales(
        @Param("month") int month,
        @Param("year") int year,
        @Param("type") String type
    );

    /**
     * Find the minimum ticket price for an event.
     * Checks all price categories in the event's location sectors.
     *
     * @param eventId the event ID
     * @return the minimum base price in cents, or null if no prices exist
     */
    @Query("SELECT MIN(pc.basePrice) FROM Event e "
        + "JOIN e.location l "
        + "JOIN l.sectors s "
        + "JOIN PriceCategory pc ON pc.sector = s "
        + "WHERE e.id = :eventId")
    Integer findMinPriceForEvent(@Param("eventId") Long eventId);
}