package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SimpleEventDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
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
    controllers = LocationEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class LocationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private EventService eventService;

    @Test
    void findAll_returnsOk_andMapsList() throws Exception {
        SimpleLocationDto dto1 = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );
        SimpleLocationDto dto2 = new SimpleLocationDto(
            2L, "Arena", 1030, "Vienna", "Baumgasse", "80",
            "BOTTOM", "Bühne", 120, 250, 2, 6, 2, 12, 50, 100, 10
        );

        when(locationService.findAllAsDto()).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/locations").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Stadthalle"))
            .andExpect(jsonPath("$[0].city").value("Vienna"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Arena"));
    }

    @Test
    void findById_returnsOk_andMapsDetailed() throws Exception {
        DetailedLocationDto detailed = new DetailedLocationDto(
            1L,
            "Stadthalle",
            1150,
            "Vienna",
            "Vogelweidplatz",
            "14",
            List.of(),
            "TOP",
            "Stage",
            100,
            200,
            1,
            5,
            1,
            10,
            null,
            null,
            null
        );

        when(locationService.findByIdAsDto(1L)).thenReturn(detailed);

        mockMvc.perform(get("/api/v1/locations/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Stadthalle"))
            .andExpect(jsonPath("$.zipCode").value(1150))
            .andExpect(jsonPath("$.city").value("Vienna"))
            .andExpect(jsonPath("$.street").value("Vogelweidplatz"))
            .andExpect(jsonPath("$.streetNumber").value("14"))
            .andExpect(jsonPath("$.stagePosition").value("TOP"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        LocationCreateDto createDto = new LocationCreateDto(
            "New Venue",
            1010,
            "Vienna",
            "Stephansplatz",
            "1"
        );

        SimpleLocationDto resultDto = new SimpleLocationDto(
            10L, "New Venue", 1010, "Vienna", "Stephansplatz", "1",
            "TOP", "Stage", null, null, null, null, null, null, null, null, null
        );

        when(locationService.createFromDto(createDto)).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("New Venue"))
            .andExpect(jsonPath("$.zipCode").value(1010));
    }

    @Test
    void searchLocations_byName_returnsOk() throws Exception {
        SimpleLocationDto dto = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );

        when(locationService.searchLocationsAsDto("Stadt", null, null, null))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/locations/search")
                .param("name", "Stadt")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Stadthalle"));
    }

    @Test
    void searchLocations_byCity_returnsOk() throws Exception {
        SimpleLocationDto dto1 = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );
        SimpleLocationDto dto2 = new SimpleLocationDto(
            2L, "Arena", 1030, "Vienna", "Baumgasse", "80",
            "BOTTOM", "Bühne", 120, 250, 2, 6, 2, 12, 50, 100, 10
        );

        when(locationService.searchLocationsAsDto(null, null, "Vienna", null))
            .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/locations/search")
                .param("city", "Vienna")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].city").value("Vienna"))
            .andExpect(jsonPath("$[1].city").value("Vienna"));
    }

    @Test
    void searchLocations_byZipCode_returnsOk() throws Exception {
        SimpleLocationDto dto = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, null, null, null
        );

        when(locationService.searchLocationsAsDto(null, null, null, 1150))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/locations/search")
                .param("zipCode", "1150")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].zipCode").value(1150));
    }

    @Test
    void getEventsByLocation_returnsOk() throws Exception {
        SimpleEventDto event1 = new SimpleEventDto(
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

        when(eventService.findByLocationIdAsDto(1L)).thenReturn(List.of(event1));

        mockMvc.perform(get("/api/v1/locations/1/events")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Rock Concert"))
            .andExpect(jsonPath("$[0].locationName").value("Vienna"));
    }
}