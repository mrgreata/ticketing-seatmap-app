package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.TopTenEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatStatus;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatmapSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.EventMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SeatMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.validators.EventValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private LocationService locationService;

    @Mock
    private ArtistService artistService;

    @Mock
    private EventValidator eventValidator;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event event1;
    private Event event2;
    private Location location;
    private Artist artist1;
    private Artist artist2;
    private Sector sector;
    private Seat seat1;
    private Seat seat2;
    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    void setup() {
        location = new Location();
        location.setId(1L);
        location.setName("Stadthalle");
        location.setCity("Vienna");
        location.setStagePosition("TOP");
        location.setStageLabel("Stage");
        location.setStageRowStart(1);
        location.setStageRowEnd(5);
        location.setStageColStart(1);
        location.setStageColEnd(10);
        location.setStageHeightPx(100);
        location.setStageWidthPx(200);

        artist1 = new Artist();
        artist1.setId(10L);
        artist1.setName("Ed Sheeran");

        artist2 = new Artist();
        artist2.setId(11L);
        artist2.setName("Taylor Swift");

        event1 = new Event();
        event1.setId(1L);
        event1.setTitle("Rock Concert");
        event1.setType("Concert");
        event1.setDurationMinutes(120);
        event1.setDescription("Great concert");
        event1.setDateTime(LocalDateTime.now().plusDays(10));
        event1.setLocation(location);
        event1.setArtists(List.of(artist1));
        event1.setTickets(new ArrayList<>());

        event2 = new Event();
        event2.setId(2L);
        event2.setTitle("Jazz Night");
        event2.setType("Concert");
        event2.setDurationMinutes(90);
        event2.setDateTime(LocalDateTime.now().plusDays(20));
        event2.setLocation(location);
        event2.setArtists(List.of(artist2));
        event2.setTickets(new ArrayList<>());

        sector = new Sector();
        sector.setId(5L);
        sector.setName("VIP");
        sector.setLocation(location);

        PriceCategory priceCategory = new PriceCategory();
        priceCategory.setId(1L);
        priceCategory.setBasePrice(5000);
        priceCategory.setDescription("Middle Price");

        seat1 = new Seat();
        seat1.setId(100L);
        seat1.setRowNumber(1);
        seat1.setSeatNumber(1);
        seat1.setSector(sector);
        seat1.setPriceCategory(priceCategory);

        seat2 = new Seat();
        seat2.setId(101L);
        seat2.setRowNumber(1);
        seat2.setSeatNumber(2);
        seat2.setSector(sector);
        seat2.setPriceCategory(priceCategory);

        ticket1 = new Ticket(location, seat1, event1);
        ticket1.setId(1000L);
        ticket1.setInvoice(null);
        ticket1.setReservation(null);

        ticket2 = new Ticket(location, seat2, event1);
        ticket2.setId(1001L);
        ticket2.setInvoice(null);
        ticket2.setReservation(null);
    }

    @Test
    void findAll_returnsAllEventsOrderedByDate() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1, event2));

        List<Event> result = eventService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(event1, event2);
        verify(eventRepository).findAllByOrderByDateTimeAsc();
    }

    @Test
    void findById_existingId_returnsEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        Event result = eventService.findById(1L);

        assertThat(result).isEqualTo(event1);
        assertThat(result.getTitle()).isEqualTo("Rock Concert");
        verify(eventRepository).findById(1L);
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found");
    }

    @Test
    void create_validEvent_savesSuccessfully() {
        Event newEvent = new Event();
        newEvent.setTitle("New Event");
        newEvent.setDateTime(LocalDateTime.now().plusDays(5));
        newEvent.setLocation(location);

        doNothing().when(eventValidator).validateEvent(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(newEvent);

        Event result = eventService.create(newEvent);

        assertThat(result).isEqualTo(newEvent);
        verify(eventValidator).validateEvent(newEvent);
        verify(eventRepository).save(newEvent);
    }

    @Test
    void create_pastDate_throwsValidationException() {
        Event invalidEvent = new Event();
        invalidEvent.setTitle("Past Event");
        invalidEvent.setDateTime(LocalDateTime.now().minusDays(1));
        invalidEvent.setLocation(location);

        doThrow(new ValidationException("Event validation failed",
            List.of("Das Datum muss in der Zukunft liegen")))
            .when(eventValidator).validateEvent(invalidEvent);

        assertThatThrownBy(() -> eventService.create(invalidEvent))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_nullLocation_throwsValidationException() {
        Event invalidEvent = new Event();
        invalidEvent.setTitle("No Location");
        invalidEvent.setDateTime(LocalDateTime.now().plusDays(5));
        invalidEvent.setLocation(null);

        doThrow(new ValidationException("Event validation failed",
            List.of("Ort ist erforderlich")))
            .when(eventValidator).validateEvent(invalidEvent);

        assertThatThrownBy(() -> eventService.create(invalidEvent))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_nullTitle_throwsValidationException() {
        Event invalidEvent = new Event();
        invalidEvent.setTitle(null);
        invalidEvent.setDateTime(LocalDateTime.now().plusDays(5));
        invalidEvent.setLocation(location);

        doThrow(new ValidationException("Event validation failed",
            List.of("Titel darf nicht leer sein")))
            .when(eventValidator).validateEvent(invalidEvent);

        assertThatThrownBy(() -> eventService.create(invalidEvent))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_blankTitle_throwsValidationException() {
        Event invalidEvent = new Event();
        invalidEvent.setTitle("   ");
        invalidEvent.setDateTime(LocalDateTime.now().plusDays(5));
        invalidEvent.setLocation(location);

        doThrow(new ValidationException("Event validation failed",
            List.of("Titel darf nicht leer sein")))
            .when(eventValidator).validateEvent(invalidEvent);

        assertThatThrownBy(() -> eventService.create(invalidEvent))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void update_existingEvent_updatesSuccessfully() {
        event1.setTitle("Updated Title");
        when(eventRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventValidator).validateEvent(event1);
        when(eventRepository.save(event1)).thenReturn(event1);

        Event result = eventService.update(event1);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(eventRepository).existsById(1L);
        verify(eventValidator).validateEvent(event1);
        verify(eventRepository).save(event1);
    }

    @Test
    void update_nonExistingEvent_throwsNotFoundException() {
        event1.setId(99L);
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.update(event1))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found");
    }

    @Test
    void delete_eventWithoutTickets_deletesSuccessfully() {
        event1.setTickets(new ArrayList<>());
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        eventService.delete(1L);

        verify(eventRepository).deleteById(1L);
    }

    @Test
    void delete_eventWithTickets_throwsValidationException() {
        event1.setTickets(List.of(ticket1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        assertThatThrownBy(() -> eventService.delete(1L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Die Veranstaltung kann nicht gelöscht werden, da bereits Tickets existieren.");
    }

    @Test
    void delete_nonExistingEvent_throwsNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchByTitle_findsMatchingEvents() {
        when(eventRepository.findByTitleContainingIgnoreCase("Rock"))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByTitle("Rock");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Rock Concert");
    }

    @Test
    void searchByType_findsMatchingEvents() {
        when(eventRepository.findByTypeIgnoreCase("Concert"))
            .thenReturn(List.of(event1, event2));

        List<Event> result = eventService.searchByType("Concert");

        assertThat(result).hasSize(2);
    }

    @Test
    void searchByDuration_findsMatchingEvents() {
        when(eventRepository.findByDurationBetween(90, 150))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByDuration(120, 30);

        assertThat(result).hasSize(1);
        verify(eventRepository).findByDurationBetween(90, 150);
    }

    @Test
    void searchByDescription_findsMatchingEvents() {
        when(eventRepository.findByDescriptionContainingIgnoreCase("Great"))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByDescription("Great");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchEvents_findsMatchingEvents() {
        when(eventRepository.searchEvents("concert"))
            .thenReturn(List.of(event1, event2));

        List<Event> result = eventService.searchEvents("concert");

        assertThat(result).hasSize(2);
    }

    @Test
    void findByArtistId_findsMatchingEvents() {
        when(eventRepository.findByArtistId(10L))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.findByArtistId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtists()).contains(artist1);
    }

    @Test
    void findByLocationId_findsMatchingEvents() {
        when(eventRepository.findByLocationId(1L))
            .thenReturn(List.of(event1, event2));

        List<Event> result = eventService.findByLocationId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void searchByDateRange_findsMatchingEvents() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(15);

        when(eventRepository.findByDateTimeBetween(start, end))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByDateRange(start, end);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByCriteria_findsMatchingEvents() {
        when(eventRepository.searchEventsByCriteria(
            "Rock", "Concert", 90, 150, null, null, 1L))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByCriteria(
            "Rock", "Concert", 90, 150, null, null, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByTimeAndPrice_findsMatchingEvents() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(30);

        when(eventRepository.searchEventsByTimeAndPrice(start, end, 1000, 10000, 1L))
            .thenReturn(List.of(event1));

        List<Event> result = eventService.searchByTimeAndPrice(start, end, 1000, 10000, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void findTopTenByTicketSales_returnsTopEvents() {
        Object[] row1 = {1L, "Rock Concert", "Concert", 500L};
        Object[] row2 = {2L, "Jazz Night", "Concert", 300L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        when(eventRepository.findTopTenEventsByTicketSales(6, 2026, null))
            .thenReturn(rows);

        List<TopTenEventDto> result = eventService.findTopTenByTicketSales(6, 2026, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).eventId()).isEqualTo(1L);
        assertThat(result.get(0).title()).isEqualTo("Rock Concert");
        assertThat(result.get(0).ticketsSold()).isEqualTo(500L);
    }

    @Test
    void findTopTenByTicketSales_withType_filtersResults() {
        Object[] row1 = {1L, "Rock Concert", "Concert", 500L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row1);

        when(eventRepository.findTopTenEventsByTicketSales(6, 2026, "Concert"))
            .thenReturn(rows);

        List<TopTenEventDto> result = eventService.findTopTenByTicketSales(6, 2026, "Concert");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("Concert");
    }

    @Test
    void getSeatmap_returnsCompleteSeatmap() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(sectorRepository.findByLocationId(1L)).thenReturn(List.of(sector));
        when(seatRepository.findBySectorIdIn(List.of(5L))).thenReturn(List.of(seat1, seat2));
        when(ticketRepository.findByEventIdAndSeatIdIn(eq(1L), anyList())).thenReturn(List.of());

        SeatmapSeatDto seatDto1 = new SeatmapSeatDto();
        seatDto1.setId(100L);
        seatDto1.setRowNumber(1);
        seatDto1.setSeatNumber(1);
        seatDto1.setStatus(SeatStatus.FREE);
        seatDto1.setPriceCategory("middle");
        seatDto1.setSectorId(5L);

        SeatmapSeatDto seatDto2 = new SeatmapSeatDto();
        seatDto2.setId(101L);
        seatDto2.setRowNumber(1);
        seatDto2.setSeatNumber(2);
        seatDto2.setStatus(SeatStatus.FREE);
        seatDto2.setPriceCategory("middle");
        seatDto2.setSectorId(5L);

        when(seatMapper.seatToSeatmapSeatDto(eq(seat1), eq(SeatStatus.FREE))).thenReturn(seatDto1);
        when(seatMapper.seatToSeatmapSeatDto(eq(seat2), eq(SeatStatus.FREE))).thenReturn(seatDto2);

        SeatmapDto result = eventService.getSeatmap(1L);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo(1L);
        assertThat(result.getSeats()).hasSize(2);
        assertThat(result.getStagePosition()).isEqualTo("TOP");
        assertThat(result.getStageLabel()).isEqualTo("Stage");
        verify(eventRepository).findById(1L);
    }

    @Test
    void getSeatmap_eventNotFound_throwsNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getSeatmap(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void uploadImage_validImage_savesSuccessfully() throws IOException {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        doNothing().when(eventValidator).validateImage(imageFile);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(eventRepository.save(event1)).thenReturn(event1);

        eventService.uploadImage(1L, imageFile);

        verify(eventValidator).validateImage(imageFile);
        verify(eventRepository).save(event1);
        assertThat(event1.getImage()).isNotNull();
        assertThat(event1.getImageContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void uploadImage_nullImage_throwsValidationException() {
        doThrow(new ValidationException("Image validation failed",
            List.of("Bild darf nicht leer sein")))
            .when(eventValidator).validateImage(null);

        assertThatThrownBy(() -> eventService.uploadImage(1L, null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadImage_emptyImage_throwsValidationException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", new byte[0]
        );

        doThrow(new ValidationException("Image validation failed",
            List.of("Bild darf nicht leer sein")))
            .when(eventValidator).validateImage(emptyFile);

        assertThatThrownBy(() -> eventService.uploadImage(1L, emptyFile))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadImage_tooLarge_throwsValidationException() {
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", largeContent
        );

        doThrow(new ValidationException("Image validation failed",
            List.of("Bild darf maximal 5 MB groß sein")))
            .when(eventValidator).validateImage(largeFile);

        assertThatThrownBy(() -> eventService.uploadImage(1L, largeFile))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadImage_invalidContentType_throwsValidationException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "image", "test.pdf", "application/pdf", "test content".getBytes()
        );

        doThrow(new ValidationException("Image validation failed",
            List.of("Nur JPG, PNG und WebP Bilder sind erlaubt")))
            .when(eventValidator).validateImage(invalidFile);

        assertThatThrownBy(() -> eventService.uploadImage(1L, invalidFile))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadImage_eventNotFound_throwsNotFoundException() {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "test".getBytes()
        );

        doNothing().when(eventValidator).validateImage(imageFile);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.uploadImage(99L, imageFile))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getImage_existingImage_returnsImageBytes() {
        byte[] imageData = "image data".getBytes();
        event1.setImage(imageData);
        event1.setImageContentType("image/jpeg");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        byte[] result = eventService.getImage(1L);

        assertThat(result).isEqualTo(imageData);
    }

    @Test
    void getImage_noImage_throwsNotFoundException() {
        event1.setImage(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        assertThatThrownBy(() -> eventService.getImage(1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("kein Bild für diese Veranstaltung gefunden");
    }

    @Test
    void getImage_eventNotFound_throwsNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getImage(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getImageContentType_existingImage_returnsContentType() {
        event1.setImage("image data".getBytes());
        event1.setImageContentType("image/png");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        String result = eventService.getImageContentType(1L);

        assertThat(result).isEqualTo("image/png");
    }

    @Test
    void getImageContentType_noImage_throwsNotFoundException() {
        event1.setImage(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        assertThatThrownBy(() -> eventService.getImageContentType(1L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findMinPriceForEvent_returnsMinPrice() {
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        Integer result = eventService.findMinPriceForEvent(1L);

        assertThat(result).isEqualTo(5000);
    }

    @Test
    void findAllAsDto_enrichesWithPrices() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1));

        SimpleEventDto simpleDto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.findAllAsDto(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).minPrice()).isEqualTo(5000);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByIdAsDto_enrichesWithPrice() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        SimpleLocationDto locationDto = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Street", "1",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );

        DetailedEventDto detailedDto = new DetailedEventDto(
            1L, "Rock Concert", "Concert", 120, "Great concert",
            event1.getDateTime(), locationDto, List.of(), 500, null
        );

        when(eventMapper.toDetailed(event1)).thenReturn(detailedDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        DetailedEventDto result = eventService.findByIdAsDto(1L);

        assertThat(result.minPrice()).isEqualTo(5000);
    }

    @Test
    void createFromDto_createsEventWithArtists() {
        EventCreateDto dto = new EventCreateDto(
            "New Concert",
            "Concert",
            120,
            "Description",
            LocalDateTime.now().plusDays(10),
            1L,
            List.of(10L)
        );

        Event mappedEvent = new Event();
        mappedEvent.setTitle("New Concert");
        mappedEvent.setDateTime(LocalDateTime.now().plusDays(10));

        Event savedEvent = new Event();
        savedEvent.setId(50L);
        savedEvent.setTitle("New Concert");
        savedEvent.setDateTime(LocalDateTime.now().plusDays(10));
        savedEvent.setLocation(location);
        savedEvent.setArtists(List.of(artist1));
        savedEvent.setTickets(new ArrayList<>());

        SimpleEventDto simpleDto = new SimpleEventDto(
            50L, "New Concert", "Concert", 120,
            savedEvent.getDateTime(), "Vienna", null, null, null
        );

        doNothing().when(eventValidator).validateForCreate(dto);
        when(eventMapper.fromCreateDto(dto)).thenReturn(mappedEvent);
        when(locationService.findById(1L)).thenReturn(location);
        when(artistService.findById(10L)).thenReturn(artist1);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(eventMapper.toSimple(savedEvent)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(50L)).thenReturn(5000);

        SimpleEventDto result = eventService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.title()).isEqualTo("New Concert");
        verify(eventValidator).validateForCreate(dto);
        verify(locationService).findById(1L);
        verify(artistService).findById(10L);
    }

    @Test
    void updateFromDto_updatesEventWithArtists() {
        EventUpdateDto dto = new EventUpdateDto(
            1L,
            "Updated Title",
            "Concert",
            150,
            "Updated description",
            LocalDateTime.now().plusDays(15),
            1L,
            List.of(11L)
        );

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Old Title");
        existingEvent.setType("Old Type");
        existingEvent.setDurationMinutes(120);
        existingEvent.setDescription("Old description");
        existingEvent.setDateTime(LocalDateTime.now().plusDays(10));
        existingEvent.setLocation(location);
        existingEvent.setArtists(new ArrayList<>());
        existingEvent.setTickets(new ArrayList<>());
        existingEvent.setImage("old-image".getBytes());
        existingEvent.setImageContentType("image/jpeg");

        Event updatedEvent = new Event();
        updatedEvent.setId(1L);
        updatedEvent.setTitle("Updated Title");
        updatedEvent.setType("Concert");
        updatedEvent.setDurationMinutes(150);
        updatedEvent.setDescription("Updated description");
        updatedEvent.setDateTime(dto.dateTime());
        updatedEvent.setLocation(location);
        updatedEvent.setArtists(List.of(artist2));
        updatedEvent.setTickets(new ArrayList<>());
        updatedEvent.setImage("old-image".getBytes());
        updatedEvent.setImageContentType("image/jpeg");

        SimpleLocationDto locationDto = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Street", "1",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );

        DetailedEventDto detailedDto = new DetailedEventDto(
            1L, "Updated Title", "Concert", 150, "Updated description",
            updatedEvent.getDateTime(), locationDto, List.of(), 500, null
        );

        doNothing().when(eventValidator).validateForUpdate(dto);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(locationService.findById(1L)).thenReturn(location);
        when(artistService.findById(11L)).thenReturn(artist2);
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);
        when(eventMapper.toDetailed(updatedEvent)).thenReturn(detailedDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        DetailedEventDto result = eventService.updateFromDto(1L, dto);

        assertThat(result.title()).isEqualTo("Updated Title");
        verify(eventValidator).validateForUpdate(dto);
        verify(eventRepository).findById(1L);
        verify(artistService).findById(11L);
    }

    @Test
    void updateFromDto_mismatchedIds_throwsValidationException() {
        EventUpdateDto dto = new EventUpdateDto(
            1L, "Title", "Type", 120, "Desc",
            LocalDateTime.now().plusDays(5), 1L, List.of()
        );

        assertThatThrownBy(() -> eventService.updateFromDto(2L, dto))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ID im Pfad und Body müssen übereinstimmen");
    }

    @Test
    void updateFromDto_preservesExistingImage() {
        EventUpdateDto dto = new EventUpdateDto(
            1L,
            "Updated Title",
            "Concert",
            120,
            "Updated description",
            LocalDateTime.now().plusDays(10),
            1L,
            List.of()
        );

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Old Title");
        existingEvent.setType("Concert");
        existingEvent.setDurationMinutes(120);
        existingEvent.setDateTime(LocalDateTime.now().plusDays(10));
        existingEvent.setLocation(location);
        existingEvent.setTickets(new ArrayList<>());
        existingEvent.setImage("existing-image-data".getBytes());
        existingEvent.setImageContentType("image/png");

        doNothing().when(eventValidator).validateForUpdate(dto);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(locationService.findById(1L)).thenReturn(location);
        when(eventRepository.existsById(1L)).thenReturn(true);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(eventCaptor.capture())).thenReturn(existingEvent);

        when(eventMapper.toDetailed(any())).thenReturn(
            new DetailedEventDto(1L, "Updated Title", "Concert", 120, "Updated description",
                existingEvent.getDateTime(), null, List.of(), 0, null)
        );
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        eventService.updateFromDto(1L, dto);

        Event savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getImage()).isNotNull();
        assertThat(savedEvent.getImage()).isEqualTo("existing-image-data".getBytes());
        assertThat(savedEvent.getImageContentType()).isEqualTo("image/png");
    }

    @Test
    void searchEventsAsDto_withCriteria_filtersAndEnriches() {
        when(eventRepository.searchEventsByCriteria(
            "Rock", "Concert", 90, 150, null, null, null))
            .thenReturn(List.of(event1));

        SimpleEventDto simpleDto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            "Rock", "Concert", 120, null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).minPrice()).isEqualTo(5000);
    }

    @Test
    void searchEventsAsDto_withPriceFilter_filtersResults() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1, event2));

        SimpleEventDto dto1 = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        SimpleEventDto dto2 = new SimpleEventDto(
            2L, "Jazz Night", "Concert", 90,
            event2.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(dto1);
        when(eventMapper.toSimple(event2)).thenReturn(dto2);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);
        when(eventRepository.findMinPriceForEvent(2L)).thenReturn(3000);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            null, null, null, null, null, null, 4000, 6000, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Rock Concert");
    }

    @Test
    void findByLocationIdAsDto_returnsEnrichedDtos() {
        when(eventRepository.findByLocationId(1L)).thenReturn(List.of(event1));

        SimpleEventDto simpleDto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        List<SimpleEventDto> result = eventService.findByLocationIdAsDto(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).minPrice()).isEqualTo(5000);
    }

    @Test
    void findByArtistIdAsDto_returnsEnrichedDtos() {
        when(eventRepository.findByArtistId(10L)).thenReturn(List.of(event1));

        SimpleEventDto simpleDto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);

        List<SimpleEventDto> result = eventService.findByArtistIdAsDto(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).minPrice()).isEqualTo(5000);
    }

    @Test
    void uploadImage_ioExceptionDuringRead_throwsValidationException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);

        when(mockFile.getBytes()).thenThrow(new IOException("Read error"));

        doNothing().when(eventValidator).validateImage(mockFile);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        assertThatThrownBy(() -> eventService.uploadImage(1L, mockFile))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Fehler beim Speichern");

        verify(eventValidator).validateImage(mockFile);
    }

    @Test
    void uploadImage_pngImage_acceptsImage() throws IOException {
        MockMultipartFile pngFile = new MockMultipartFile(
            "image", "test.png", "image/png", "png content".getBytes()
        );

        doNothing().when(eventValidator).validateImage(pngFile);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(eventRepository.save(event1)).thenReturn(event1);

        eventService.uploadImage(1L, pngFile);

        verify(eventValidator).validateImage(pngFile);
        verify(eventRepository).save(event1);
        assertThat(event1.getImageContentType()).isEqualTo("image/png");
    }

    @Test
    void uploadImage_webpImage_acceptsImage() {
        MockMultipartFile webpFile = new MockMultipartFile(
            "image", "test.webp", "image/webp", "webp content".getBytes()
        );

        doNothing().when(eventValidator).validateImage(webpFile);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(eventRepository.save(event1)).thenReturn(event1);

        eventService.uploadImage(1L, webpFile);

        verify(eventValidator).validateImage(webpFile);
        verify(eventRepository).save(event1);
        assertThat(event1.getImageContentType()).isEqualTo("image/webp");
    }

    @Test
    void getSeatmap_withSoldAndReservedSeats_marksDifferentStatuses() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(sectorRepository.findByLocationId(1L)).thenReturn(List.of(sector));
        when(seatRepository.findBySectorIdIn(List.of(5L))).thenReturn(List.of(seat1, seat2));

        Invoice invoice = new Invoice();
        ticket1.setInvoice(invoice);
        ticket1.setReservation(null);

        Reservation reservation = new Reservation();
        ticket2.setReservation(reservation);
        ticket2.setInvoice(null);

        when(ticketRepository.findByEventIdAndSeatIdIn(eq(1L), anyList()))
            .thenReturn(List.of(ticket1, ticket2));

        SeatmapSeatDto seatDto1 = new SeatmapSeatDto();
        seatDto1.setId(100L);
        seatDto1.setStatus(SeatStatus.SOLD);
        seatDto1.setSectorId(5L);

        SeatmapSeatDto seatDto2 = new SeatmapSeatDto();
        seatDto2.setId(101L);
        seatDto2.setStatus(SeatStatus.RESERVED);
        seatDto2.setSectorId(5L);

        when(seatMapper.seatToSeatmapSeatDto(eq(seat1), eq(SeatStatus.SOLD)))
            .thenReturn(seatDto1);
        when(seatMapper.seatToSeatmapSeatDto(eq(seat2), eq(SeatStatus.RESERVED)))
            .thenReturn(seatDto2);

        SeatmapDto result = eventService.getSeatmap(1L);

        assertThat(result.getSeats()).hasSize(2);
        assertThat(result.getSeats().get(0).getStatus()).isEqualTo(SeatStatus.SOLD);
        assertThat(result.getSeats().get(1).getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void getSeatmap_eventWithRunway_setsRunwayProperties() {
        event1.setId(2L);
        location.setRunwayWidthPx(100);
        location.setRunwayLengthPx(300);
        location.setRunwayOffsetPx(50);

        when(eventRepository.findById(2L)).thenReturn(Optional.of(event1));
        when(sectorRepository.findByLocationId(1L)).thenReturn(List.of(sector));
        when(seatRepository.findBySectorIdIn(anyList())).thenReturn(List.of(seat1));
        when(ticketRepository.findByEventIdAndSeatIdIn(eq(2L), anyList())).thenReturn(List.of());

        SeatmapSeatDto seatDto = new SeatmapSeatDto();
        seatDto.setId(100L);
        seatDto.setStatus(SeatStatus.FREE);
        seatDto.setSectorId(5L);

        when(seatMapper.seatToSeatmapSeatDto(any(), any())).thenReturn(seatDto);

        SeatmapDto result = eventService.getSeatmap(2L);

        assertThat(result.getRunwayWidthPx()).isEqualTo(100);
        assertThat(result.getRunwayLengthPx()).isEqualTo(300);
        assertThat(result.getRunwayOffsetPx()).isEqualTo(50);
    }

    @Test
    void getSeatmap_eventWithoutRunway_nullsRunwayProperties() {
        event1.setId(1L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(sectorRepository.findByLocationId(1L)).thenReturn(List.of(sector));
        when(seatRepository.findBySectorIdIn(anyList())).thenReturn(List.of(seat1));
        when(ticketRepository.findByEventIdAndSeatIdIn(eq(1L), anyList())).thenReturn(List.of());

        SeatmapSeatDto seatDto = new SeatmapSeatDto();
        seatDto.setId(100L);
        seatDto.setStatus(SeatStatus.FREE);
        seatDto.setSectorId(5L);

        when(seatMapper.seatToSeatmapSeatDto(any(), any())).thenReturn(seatDto);

        SeatmapDto result = eventService.getSeatmap(1L);

        assertThat(result.getRunwayWidthPx()).isNull();
        assertThat(result.getRunwayLengthPx()).isNull();
        assertThat(result.getRunwayOffsetPx()).isNull();
    }

    @Test
    void searchEventsAsDto_minPriceNull_filtersOutEvent() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1));

        SimpleEventDto dto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(dto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(null);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            null, null, null, null, null, null, 1000, 5000, pageable
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void searchEventsAsDto_noPriceFilter_includesEventWithNullPrice() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1));

        SimpleEventDto dto = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, null, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(dto);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(null);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            null, null, null, null, null, null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createFromDto_nullArtistIds_createsWithoutArtists() {
        EventCreateDto dto = new EventCreateDto(
            "Event",
            "Concert",
            120,
            "Desc",
            LocalDateTime.now().plusDays(10),
            1L,
            null
        );

        Event mappedEvent = new Event();
        mappedEvent.setTitle("Event");
        mappedEvent.setDateTime(LocalDateTime.now().plusDays(10));
        mappedEvent.setTickets(new ArrayList<>());

        Event savedEvent = new Event();
        savedEvent.setId(99L);
        savedEvent.setTitle("Event");
        savedEvent.setDateTime(LocalDateTime.now().plusDays(10));
        savedEvent.setLocation(location);
        savedEvent.setTickets(new ArrayList<>());

        SimpleEventDto simpleDto = new SimpleEventDto(
            99L, "Event", "Concert", 120,
            savedEvent.getDateTime(), "Vienna", null, 5000, null
        );

        doNothing().when(eventValidator).validateForCreate(dto);
        when(eventMapper.fromCreateDto(dto)).thenReturn(mappedEvent);
        when(locationService.findById(1L)).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(eventMapper.toSimple(savedEvent)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(99L)).thenReturn(5000);

        SimpleEventDto result = eventService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(99L);
        verify(artistService, never()).findById(anyLong());
    }

    @Test
    void createFromDto_emptyArtistIds_createsWithoutArtists() {
        EventCreateDto dto = new EventCreateDto(
            "Event",
            "Concert",
            120,
            "Desc",
            LocalDateTime.now().plusDays(10),
            1L,
            List.of()
        );

        Event mappedEvent = new Event();
        mappedEvent.setTitle("Event");
        mappedEvent.setDateTime(LocalDateTime.now().plusDays(10));
        mappedEvent.setTickets(new ArrayList<>());

        Event savedEvent = new Event();
        savedEvent.setId(99L);
        savedEvent.setTitle("Event");
        savedEvent.setDateTime(LocalDateTime.now().plusDays(10));
        savedEvent.setLocation(location);
        savedEvent.setTickets(new ArrayList<>());

        SimpleEventDto simpleDto = new SimpleEventDto(
            99L, "Event", "Concert", 120,
            savedEvent.getDateTime(), "Vienna", null, 5000, null
        );

        doNothing().when(eventValidator).validateForCreate(dto);
        when(eventMapper.fromCreateDto(dto)).thenReturn(mappedEvent);
        when(locationService.findById(1L)).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(eventMapper.toSimple(savedEvent)).thenReturn(simpleDto);
        when(eventRepository.findMinPriceForEvent(99L)).thenReturn(5000);

        SimpleEventDto result = eventService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(99L);
        verify(artistService, never()).findById(anyLong());
    }

    @Test
    void searchEventsAsDto_withPriceMinOnly_filtersCorrectly() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1, event2));

        SimpleEventDto dto1 = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, 5000, null
        );

        SimpleEventDto dto2 = new SimpleEventDto(
            2L, "Jazz Night", "Concert", 90,
            event2.getDateTime(), "Vienna", null, 3000, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(dto1);
        when(eventMapper.toSimple(event2)).thenReturn(dto2);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);
        when(eventRepository.findMinPriceForEvent(2L)).thenReturn(3000);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            null, null, null, null, null, null, 4000, null, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Rock Concert");
    }

    @Test
    void searchEventsAsDto_withPriceMaxOnly_filtersCorrectly() {
        when(eventRepository.findAllByOrderByDateTimeAsc()).thenReturn(List.of(event1, event2));

        SimpleEventDto dto1 = new SimpleEventDto(
            1L, "Rock Concert", "Concert", 120,
            event1.getDateTime(), "Vienna", null, 5000, null
        );

        SimpleEventDto dto2 = new SimpleEventDto(
            2L, "Jazz Night", "Concert", 90,
            event2.getDateTime(), "Vienna", null, 3000, null
        );

        when(eventMapper.toSimple(event1)).thenReturn(dto1);
        when(eventMapper.toSimple(event2)).thenReturn(dto2);
        when(eventRepository.findMinPriceForEvent(1L)).thenReturn(5000);
        when(eventRepository.findMinPriceForEvent(2L)).thenReturn(3000);

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> result = eventService.searchEventsAsDto(
            null, null, null, null, null, null, null, 4000, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Jazz Night");
    }
}