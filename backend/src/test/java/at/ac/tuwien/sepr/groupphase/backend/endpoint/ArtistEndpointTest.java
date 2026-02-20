package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ArtistEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class ArtistEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private EventService eventService;

    @Test
    void findAll_returnsOk_andMapsList() throws Exception {
        SimpleArtistDto dto1 = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());
        SimpleArtistDto dto2 = new SimpleArtistDto(2L, "The Beatles", true, List.of(3L, 4L));

        when(artistService.findAllAsDto()).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/artists").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Ed Sheeran"))
            .andExpect(jsonPath("$[0].isBand").value(false))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("The Beatles"))
            .andExpect(jsonPath("$[1].isBand").value(true));
    }

    @Test
    void findById_returnsOk_andMapsDetailed() throws Exception {
        SimpleArtistDto member1 = new SimpleArtistDto(3L, "John Lennon", false, List.of());
        SimpleArtistDto member2 = new SimpleArtistDto(4L, "Paul McCartney", false, List.of());

        DetailedArtistDto detailed = new DetailedArtistDto(
            2L,
            "The Beatles",
            true,
            List.of(member1, member2),
            List.of()
        );

        when(artistService.findByIdAsDto(2L)).thenReturn(detailed);

        mockMvc.perform(get("/api/v1/artists/2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("The Beatles"))
            .andExpect(jsonPath("$.isBand").value(true))
            .andExpect(jsonPath("$.members[0].name").value("John Lennon"))
            .andExpect(jsonPath("$.members[1].name").value("Paul McCartney"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        ArtistCreateDto createDto = new ArtistCreateDto("Adele", false, null);
        SimpleArtistDto resultDto = new SimpleArtistDto(5L, "Adele", false, List.of());

        when(artistService.createFromDto(createDto)).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/artists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("Adele"));
    }

    @Test
    void searchArtists_byName_returnsOk() throws Exception {
        SimpleArtistDto dto = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());

        when(artistService.searchArtistsAsDto("Ed", true)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/artists/search")
                .param("name", "Ed")
                .param("includeBands", "true")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Ed Sheeran"));
    }

    @Test
    void searchArtists_withDefaultIncludeBands_returnsOk() throws Exception {
        SimpleArtistDto dto = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());

        when(artistService.searchArtistsAsDto("Ed", true)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/artists/search")
                .param("name", "Ed")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Ed Sheeran"));
    }

    @Test
    void getEventsByArtist_returnsOk() throws Exception {
        SimpleEventDto event1 = new SimpleEventDto(
            1L,
            "Ed Sheeran Live",
            "Concert",
            120,
            LocalDateTime.of(2026, 6, 15, 20, 0),
            "Vienna",
            null,
            5000,
            null
        );


        when(eventService.findByArtistIdAsDto(1L)).thenReturn(List.of(event1));

        mockMvc.perform(get("/api/v1/artists/1/events")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Ed Sheeran Live"))
            .andExpect(jsonPath("$[0].locationName").value("Vienna"));
    }
}