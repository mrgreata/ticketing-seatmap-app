package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatStatus;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatmapSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.TopTenEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.EventMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SeatMapper;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;

import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.validators.EventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass());

    private final EventRepository eventRepository;
    private final SectorRepository sectorRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatMapper seatMapper;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;
    private final LocationService locationService;
    private final ArtistService artistService;

    public EventServiceImpl(EventRepository eventRepository,
                            SectorRepository sectorRepository,
                            SeatRepository seatRepository,
                            TicketRepository ticketRepository,
                            SeatMapper seatMapper,
                            EventMapper eventMapper,
                            EventValidator eventValidator,
                            LocationService locationService,
                            ArtistService artistService) {
        this.eventRepository = eventRepository;
        this.sectorRepository = sectorRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.seatMapper = seatMapper;
        this.eventMapper = eventMapper;
        this.eventValidator = eventValidator;
        this.locationService = locationService;
        this.artistService = artistService;
    }

    @Override
    public List<Event> findAll() {
        LOGGER.debug("Find all events ordered by date");
        return eventRepository.findAllByOrderByDateTimeAsc();
    }

    @Override
    public Event findById(Long id) {
        LOGGER.debug("Find event by id {}", id);
        return eventRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Event not found: " + id));
    }

    @Override
    public Event create(Event event) {
        LOGGER.debug("Create event {}", event);
        validateEventData(event);
        return eventRepository.save(event);
    }

    @Override
    public Event update(Event event) {
        LOGGER.debug("Update event {}", event.getId());

        if (!eventRepository.existsById(event.getId())) {
            throw new NotFoundException("Event not found: " + event.getId());
        }

        validateEventData(event);
        return eventRepository.save(event);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LOGGER.debug("Delete event {}", id);

        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Event not found: " + id));

        if (!event.getTickets().isEmpty()) {
            throw new ValidationException("Die Veranstaltung kann nicht gelöscht werden, da bereits Tickets existieren.");
        }

        eventRepository.deleteById(id);
    }

    @Override
    public void validateEventData(Event event) {
        LOGGER.trace("validateEventData({})", event);
        eventValidator.validateEvent(event);
    }

    @Override
    public List<Event> searchByTitle(String title) {
        LOGGER.debug("Search events by title: {}", title);
        return eventRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Event> searchByType(String type) {
        LOGGER.debug("Search events by type: {}", type);
        return eventRepository.findByTypeIgnoreCase(type);
    }

    @Override
    public List<Event> searchByDuration(int duration, int tolerance) {
        LOGGER.debug("Search events by duration {} +/- {}", duration, tolerance);
        return eventRepository.findByDurationBetween(duration - tolerance, duration + tolerance);
    }

    @Override
    public List<Event> searchByDescription(String description) {
        LOGGER.debug("Search events by description: {}", description);
        return eventRepository.findByDescriptionContainingIgnoreCase(description);
    }

    @Override
    public List<Event> searchEvents(String searchTerm) {
        LOGGER.debug("Search events with term: {}", searchTerm);
        return eventRepository.searchEvents(searchTerm);
    }

    @Override
    public List<Event> findByArtistId(Long artistId) {
        LOGGER.debug("Find events by artist id: {}", artistId);
        return eventRepository.findByArtistId(artistId);
    }

    @Override
    public List<Event> findByLocationId(Long locationId) {
        LOGGER.debug("Find events by location id: {}", locationId);
        return eventRepository.findByLocationId(locationId);
    }

    @Override
    public List<Event> searchByDateRange(LocalDateTime start, LocalDateTime end) {
        LOGGER.debug("Search events between {} and {}", start, end);
        return eventRepository.findByDateTimeBetween(start, end);
    }

    @Override
    public List<Event> searchByCriteria(
        String title,
        String type,
        Integer minDuration,
        Integer maxDuration,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long locationId) {
        LOGGER.debug("Search events by multiple criteria");
        return eventRepository.searchEventsByCriteria(
            title, type, minDuration, maxDuration, startDate, endDate, locationId);
    }

    @Override
    public List<Event> searchByTimeAndPrice(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer minPrice,
        Integer maxPrice,
        Long locationId) {
        LOGGER.debug("Search events by time and price: dates={}-{}, price={}-{}",
            startDate, endDate, minPrice, maxPrice);
        return eventRepository.searchEventsByTimeAndPrice(
            startDate, endDate, minPrice, maxPrice, locationId);
    }

    @Override
    public List<TopTenEventDto> findTopTenByTicketSales(int month, int year, String type) {
        LOGGER.debug("Find top 10 events for {}/{} type: {}", month, year, type);

        List<Object[]> results = eventRepository.findTopTenEventsByTicketSales(month, year, type);

        return results.stream()
            .map(row -> new TopTenEventDto(
                (Long) row[0],
                (String) row[1],
                (String) row[2],
                (Long) row[3]
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatmapDto getSeatmap(Long eventId) {

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        Long locationId = event.getLocation().getId();

        List<Sector> sectors = sectorRepository.findByLocationId(locationId);
        List<Long> sectorIds = sectors.stream()
            .map(Sector::getId)
            .toList();

        List<Seat> seats = seatRepository.findBySectorIdIn(sectorIds);

        int minRow = seats.stream().mapToInt(Seat::getRowNumber).min().orElse(-1);
        int maxRow = seats.stream().mapToInt(Seat::getRowNumber).max().orElse(-1);

        LOGGER.info("Seat rows min/max = {}/{}", minRow, maxRow);
        LOGGER.info("Sectors: {}", sectors.stream().map(Sector::getName).toList());
        LOGGER.info("PriceCats: {}", seats.stream()
            .map(s -> s.getPriceCategory() != null ? s.getPriceCategory().getDescription() : "NULL")
            .collect(Collectors.groupingBy(x -> x, Collectors.counting()))
        );


        List<Long> seatIds = seats.stream()
            .map(Seat::getId)
            .toList();

        List<Ticket> tickets = ticketRepository.findByEventIdAndSeatIdIn(eventId, seatIds);

        Set<Long> soldSeatIds = tickets.stream()
            .filter(t -> t.getInvoice() != null)
            .map(t -> t.getSeat().getId())
            .collect(Collectors.toSet());

        Set<Long> reservedSeatIds = tickets.stream()
            .filter(t -> t.getReservation() != null && t.getInvoice() == null)
            .map(t -> t.getSeat().getId())
            .collect(Collectors.toSet());

        List<SeatmapSeatDto> seatDtos = seats.stream()
            .map(seat -> {
                SeatStatus status;
                if (soldSeatIds.contains(seat.getId())) {
                    status = SeatStatus.SOLD;
                } else if (reservedSeatIds.contains(seat.getId())) {
                    status = SeatStatus.RESERVED;
                } else {
                    status = SeatStatus.FREE;
                }
                return seatMapper.seatToSeatmapSeatDto(seat, status);
            })
            .toList();

        SeatmapDto dto = new SeatmapDto();
        dto.setEventId(eventId);
        dto.setSeats(seatDtos);

        LOGGER.info("Seatmap eventId={}, sectors={}, seats={}, tickets={}",
            eventId, sectors.size(), seats.size(), tickets.size());
        LOGGER.info("Seatmap status counts: sold={}, reserved={}",
            soldSeatIds.size(), reservedSeatIds.size());

        dto.setStagePosition(event.getLocation().getStagePosition());
        dto.setStageLabel(event.getLocation().getStageLabel());

        dto.setStageRowStart(event.getLocation().getStageRowStart());
        dto.setStageRowEnd(event.getLocation().getStageRowEnd());
        dto.setStageColStart(event.getLocation().getStageColStart());
        dto.setStageColEnd(event.getLocation().getStageColEnd());
        dto.setStageHeightPx(event.getLocation().getStageHeightPx());
        dto.setStageWidthPx(event.getLocation().getStageWidthPx());
        boolean hasRunway = Long.valueOf(2L).equals(eventId);

        if (hasRunway) {
            dto.setRunwayWidthPx(event.getLocation().getRunwayWidthPx());
            dto.setRunwayLengthPx(event.getLocation().getRunwayLengthPx());
            dto.setRunwayOffsetPx(event.getLocation().getRunwayOffsetPx());
        } else {
            dto.setRunwayWidthPx(null);
            dto.setRunwayLengthPx(null);
            dto.setRunwayOffsetPx(null);
        }


        return dto;
    }

    @Override
    public Integer findMinPriceForEvent(Long eventId) {
        LOGGER.debug("Find min price for event {}", eventId);
        return eventRepository.findMinPriceForEvent(eventId);
    }

    @Override
    @Transactional
    public void uploadImage(Long eventId, MultipartFile image) {
        eventValidator.validateImage(image);

        LOGGER.trace("uploadImage({}, {})", eventId, image.getOriginalFilename());

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event mit ID " + eventId + " nicht gefunden"));

        try {
            byte[] imageData = image.getBytes();
            String contentType = image.getContentType();

            event.setImage(imageData);
            event.setImageContentType(contentType);

            eventRepository.save(event);

            LOGGER.info("Bild für Event {} erfolgreich hochgeladen", eventId);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Hochladen des Bildes für Event {}", eventId, e);
            throw new ValidationException("Fehler beim Speichern des Bildes", List.of(e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImage(Long eventId) {
        LOGGER.debug("Get image for event {}", eventId);

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Die Veranstaltung wurde nicht gefunden"));

        if (event.getImage() == null) {
            throw new NotFoundException("Es wurde kein Bild für diese Veranstaltung gefunden");
        }

        return event.getImage();
    }

    @Override
    @Transactional(readOnly = true)
    public String getImageContentType(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Die Veranstaltung wurde nicht gefunden"));

        if (event.getImage() == null) {
            throw new NotFoundException("Es wurde kein Bild für diese Veranstaltung gefunden");
        }

        return event.getImageContentType();
    }

    @Override
    @Transactional
    public void deleteImage(Long eventId) {
        LOGGER.debug("Delete image for event {}", eventId);
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        if (event.getImage() == null) {
            throw new NotFoundException("Veranstaltung hat kein Bild");
        }

        event.setImage(null);
        event.setImageContentType(null);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SimpleEventDto> findAllAsDto(Pageable pageable) {
        LOGGER.debug("Find all events as DTOs with pagination (page={}, size={})",
            pageable.getPageNumber(), pageable.getPageSize());

        List<Event> allEvents = eventRepository.findAllByOrderByDateTimeAsc();
        List<SimpleEventDto> enrichedEvents = allEvents.stream()
            .map(this::enrichEventWithPrice)
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), enrichedEvents.size());

        List<SimpleEventDto> pageContent = enrichedEvents.subList(start, end);
        return new PageImpl<>(pageContent, pageable, enrichedEvents.size());
    }

    @Override
    @Transactional(readOnly = true)
    public DetailedEventDto findByIdAsDto(Long id) {
        LOGGER.debug("Find event by id {} as DTO with price enrichment", id);
        Event event = findById(id);
        return enrichDetailedEventWithPrice(event);
    }

    @Override
    @Transactional
    public SimpleEventDto createFromDto(EventCreateDto dto) {
        LOGGER.trace("createFromDto({})", dto);

        eventValidator.validateForCreate(dto);

        var event = eventMapper.fromCreateDto(dto);
        event.setLocation(locationService.findById(dto.locationId()));

        if (dto.artistIds() != null && !dto.artistIds().isEmpty()) {
            var artists = dto.artistIds().stream()
                .map(artistService::findById)
                .toList();
            event.setArtists(artists);
        }

        var saved = create(event);
        return enrichEventWithPrice(saved);
    }

    @Override
    @Transactional
    public DetailedEventDto updateFromDto(Long id, EventUpdateDto dto) {
        LOGGER.debug("Update event {} from DTO", id);

        eventValidator.validateForUpdate(dto);

        if (!id.equals(dto.id())) {
            throw new ValidationException("ID im Pfad und Body müssen übereinstimmen");
        }

        Event existingEvent = eventRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Veranstaltung nicht gefunden: " + id));

        existingEvent.setTitle(dto.title());
        existingEvent.setType(dto.type());
        existingEvent.setDurationMinutes(dto.durationMinutes());
        existingEvent.setDescription(dto.description());
        existingEvent.setDateTime(dto.dateTime());
        existingEvent.setLocation(locationService.findById(dto.locationId()));

        if (dto.artistIds() != null && !dto.artistIds().isEmpty()) {
            List<Artist> artists = dto.artistIds().stream()
                .map(artistService::findById)
                .collect(Collectors.toCollection(ArrayList::new));
            existingEvent.setArtists(artists);
        } else {
            existingEvent.setArtists(new ArrayList<>());
        }

        var updated = update(existingEvent);
        return enrichDetailedEventWithPrice(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SimpleEventDto> searchEventsAsDto(
        String title,
        String type,
        Integer duration,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        Long locationId,
        Integer priceMin,
        Integer priceMax,
        Pageable pageable) {

        LOGGER.debug("Search events with criteria, price filters, and pagination");

        boolean hasCriteriaFilters = title != null || type != null || duration != null
            || dateFrom != null || dateTo != null;
        boolean hasPriceFilters = priceMin != null || priceMax != null;

        List<Event> results;
        if (hasCriteriaFilters) {
            Integer minDuration = duration != null && duration > 0 ? Math.max(0, duration - 30) : null;
            Integer maxDuration = duration != null && duration > 0 ? duration + 30 : null;
            results = searchByCriteria(title, type, minDuration, maxDuration, dateFrom, dateTo, locationId);
        } else {
            results = findAll();
        }

        List<SimpleEventDto> enrichedResults = results.stream()
            .map(this::enrichEventWithPrice)
            .toList();

        if (hasPriceFilters) {
            enrichedResults = enrichedResults.stream()
                .filter(dto -> {
                    if (dto.minPrice() == null) {
                        return false;
                    }
                    boolean matchesMin = priceMin == null || dto.minPrice() >= priceMin;
                    boolean matchesMax = priceMax == null || dto.minPrice() <= priceMax;
                    return matchesMin && matchesMax;
                })
                .toList();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), enrichedResults.size());

        List<SimpleEventDto> pageContent = enrichedResults.subList(start, end);
        return new PageImpl<>(pageContent, pageable, enrichedResults.size());
    }

    private SimpleEventDto enrichEventWithPrice(Event event) {
        SimpleEventDto dto = eventMapper.toSimple(event);
        Integer minPrice = findMinPriceForEvent(event.getId());
        return new SimpleEventDto(
            dto.id(),
            dto.title(),
            dto.type(),
            dto.durationMinutes(),
            dto.dateTime(),
            dto.locationName(),
            dto.locationCity(),
            minPrice,
            dto.description()
        );
    }

    private DetailedEventDto enrichDetailedEventWithPrice(Event event) {
        DetailedEventDto dto = eventMapper.toDetailed(event);
        Integer minPrice = findMinPriceForEvent(event.getId());
        return new DetailedEventDto(
            dto.id(),
            dto.title(),
            dto.type(),
            dto.durationMinutes(),
            dto.description(),
            dto.dateTime(),
            dto.location(),
            dto.artists(),
            dto.ticketCount(),
            minPrice
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleEventDto> findByLocationIdAsDto(Long locationId) {
        LOGGER.debug("Find events by location {} as DTOs", locationId);
        return findByLocationId(locationId)
            .stream()
            .map(this::enrichEventWithPrice)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleEventDto> findByArtistIdAsDto(Long artistId) {
        LOGGER.debug("Find events by artist {} as DTOs", artistId);
        return findByArtistId(artistId)
            .stream()
            .map(this::enrichEventWithPrice)
            .toList();
    }
}