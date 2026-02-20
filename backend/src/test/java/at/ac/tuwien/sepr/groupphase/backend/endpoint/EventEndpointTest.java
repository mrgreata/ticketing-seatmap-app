package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.DetailedEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.EventUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.TopTenEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = EventEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class EventEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @Test
    void findAll_returnsOk_andMapsPagedList() throws Exception {
        SimpleEventDto dto1 = new SimpleEventDto(
            1L,
            "Rock Concert",
            "Concert",
            120,
            LocalDateTime.of(2026, 6, 15, 20, 0),
            "Vienna",
            null,
            5000,
            null
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> page = new PageImpl<>(List.of(dto1), pageable, 1);

        when(eventService.findAllAsDto(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/events")
                .param("page", "0")
                .param("size", "12")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Rock Concert"))
            .andExpect(jsonPath("$.content[0].type").value("Concert"))
            .andExpect(jsonPath("$.content[0].minPrice").value(5000))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void findAll_withDefaultParams_returnsFirstPage() throws Exception {
        SimpleEventDto dto1 = new SimpleEventDto(
            1L,
            "Rock Concert",
            "Concert",
            120,
            LocalDateTime.of(2026, 6, 15, 20, 0),
            "Vienna",
            null,
            5000,
            null
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> page = new PageImpl<>(List.of(dto1), pageable, 1);

        when(eventService.findAllAsDto(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/events").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void findById_returnsOk_andMapsDetailed() throws Exception {
        SimpleLocationDto location = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );
        SimpleArtistDto artist = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());
        DetailedEventDto detailed = new DetailedEventDto(
            1L,
            "Ed Sheeran Live",
            "Concert",
            120,
            "Amazing concert",
            LocalDateTime.of(2026, 6, 15, 20, 0),
            location,
            List.of(artist),
            500,
            5000
        );

        when(eventService.findByIdAsDto(1L)).thenReturn(detailed);

        mockMvc.perform(get("/api/v1/events/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Ed Sheeran Live"))
            .andExpect(jsonPath("$.ticketCount").value(500))
            .andExpect(jsonPath("$.location.name").value("Stadthalle"))
            .andExpect(jsonPath("$.artists[0].name").value("Ed Sheeran"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        EventCreateDto createDto = new EventCreateDto(
            "New Event",
            "Concert",
            120,
            "Description",
            LocalDateTime.of(2026, 7, 1, 19, 0),
            1L,
            List.of(1L)
        );

        SimpleEventDto resultDto = new SimpleEventDto(
            10L,
            "New Event",
            "Concert",
            120,
            LocalDateTime.of(2026, 7, 1, 19, 0),
            "Vienna",
            null,
            5000,
            null
        );

        when(eventService.createFromDto(any(EventCreateDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("New Event"));
    }

    @Test
    void update_returnsOk() throws Exception {
        EventUpdateDto updateDto = new EventUpdateDto(
            1L,
            "Updated Event",
            "Concert",
            150,
            "Updated description",
            LocalDateTime.of(2026, 7, 1, 20, 0),
            1L,
            List.of(1L)
        );

        SimpleLocationDto location = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );
        SimpleArtistDto artist = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());

        DetailedEventDto resultDto = new DetailedEventDto(
            1L,
            "Updated Event",
            "Concert",
            150,
            "Updated description",
            LocalDateTime.of(2026, 7, 1, 20, 0),
            location,
            List.of(artist),
            500,
            5000
        );

        when(eventService.updateFromDto(eq(1L), any(EventUpdateDto.class))).thenReturn(resultDto);

        mockMvc.perform(put("/api/v1/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Event"))
            .andExpect(jsonPath("$.durationMinutes").value(150));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(eventService).delete(1L);

        mockMvc.perform(delete("/api/v1/events/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getSeatmap_returnsOk() throws Exception {
        SeatmapDto seatmap = new SeatmapDto();
        seatmap.setEventId(1L);
        seatmap.setStagePosition("TOP");
        seatmap.setStageLabel("Stage");

        when(eventService.getSeatmap(1L)).thenReturn(seatmap);

        mockMvc.perform(get("/api/v1/events/1/seatmap")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventId").value(1))
            .andExpect(jsonPath("$.stagePosition").value("TOP"));
    }

    @Test
    void searchEvents_withTitle_returnsOk() throws Exception {
        SimpleEventDto dto = new SimpleEventDto(
            1L,
            "Rock Concert",
            "Concert",
            120,
            LocalDateTime.of(2026, 6, 15, 20, 0),
            "Vienna",
            null,
            5000,
            null
        );

        Pageable pageable = PageRequest.of(0, 12);
        Page<SimpleEventDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        when(eventService.searchEventsAsDto(
            eq("Rock"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/events/search")
                .param("title", "Rock")
                .param("page", "0")
                .param("size", "12")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Rock Concert"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTopTen_returnsOk() throws Exception {
        TopTenEventDto dto1 = new TopTenEventDto(1L, "Event 1", "Concert", 500L);
        TopTenEventDto dto2 = new TopTenEventDto(2L, "Event 2", "Theater", 450L);

        when(eventService.findTopTenByTicketSales(6, 2026, null))
            .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/events/top-ten")
                .param("month", "6")
                .param("year", "2026")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventId").value(1))
            .andExpect(jsonPath("$[0].ticketsSold").value(500))
            .andExpect(jsonPath("$[1].ticketsSold").value(450));
    }

    @Test
    void getTopTen_withType_returnsOk() throws Exception {
        TopTenEventDto dto = new TopTenEventDto(1L, "Event 1", "Concert", 500L);

        when(eventService.findTopTenByTicketSales(6, 2026, "Concert"))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/events/top-ten")
                .param("month", "6")
                .param("year", "2026")
                .param("type", "Concert")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("Concert"));
    }

    @Test
    void uploadImage_returnsOk() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".getBytes()
        );

        doNothing().when(eventService).uploadImage(eq(1L), any(MultipartFile.class));

        mockMvc.perform(multipart("/api/v1/events/1/image")
                .file(imageFile))
            .andExpect(status().isOk());
    }

    @Test
    void getImage_returnsImageWithHeaders() throws Exception {
        byte[] imageData = "image bytes".getBytes();

        when(eventService.getImage(1L)).thenReturn(imageData);
        when(eventService.getImageContentType(1L)).thenReturn("image/jpeg");

        mockMvc.perform(get("/api/v1/events/1/image"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andExpect(header().string("Content-Length", String.valueOf(imageData.length)))
            .andExpect(content().bytes(imageData));
    }
}