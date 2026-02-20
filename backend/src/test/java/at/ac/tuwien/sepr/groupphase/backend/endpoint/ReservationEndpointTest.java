package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ReservationMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.ReservationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ReservationEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class ReservationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReservationMapper reservationMapper;

    // ---------------- reserve ----------------
    @Test
    void reserve_returns201() throws Exception {
        List<Long> ticketIds = List.of(1L, 2L);
        SimpleReservationDto simpleDto = new SimpleReservationDto(1L, "RES_1", 1L);
        when(reservationService.create(ticketIds, "user@test.com")).thenReturn(simpleDto);
        when(reservationMapper.toSimple(any(Reservation.class))).thenReturn(simpleDto);

        mockMvc.perform(patch("/api/v1/reservations")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.reservationNumber").value("RES_1"))
            .andExpect(jsonPath("$.userId").value(1L));
    }

    // ---------------- cancelReservations ----------------
    @Test
    void cancelReservations_returns200() throws Exception {
        List<Long> ticketIds = List.of(1L, 2L);

        doNothing().when(reservationService).cancelReservations(ticketIds, "user@test.com");

        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isOk());
    }

    // ---------------- getMyReservations ----------------
    @Test
    void getMyReservations_returns200() throws Exception {
        User dummyUser = new User();
        dummyUser.setId(1L);
        dummyUser.setEmail("user@test.com");

        DetailedReservationDto dto = new DetailedReservationDto(
            10L,
            1L,
            "Concert",
            1,
            1,
            1L,
            LocalDate.now(),
            "18:00",
            "RES_1",
            100D,
            true
        );

        when(userService.findByEmail("user@test.com")).thenReturn(dummyUser);
        when(reservationService.findByUser("user@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/reservations/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ticketId").value(1L))
            .andExpect(jsonPath("$[0].reservationNumber").value("RES_1"))
            .andExpect(jsonPath("$[0].eventName").value("Concert"));
    }

    // ---------------- findById ----------------
    @Test
    void findById_returns200() throws Exception {
        SimpleReservationDto simpleDto = new SimpleReservationDto(1L, "RES_1", 1L);
        when(reservationService.findById(1L, "user@test.com"))
            .thenReturn(simpleDto);

        mockMvc.perform(get("/api/v1/reservations/1")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.reservationNumber").value("RES_1"))
            .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void findById_notMyReservation_returns403() throws Exception {
        when(reservationService.findById(99L, "user@test.com"))
            .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/reservations/99")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancelReservations_notMyTickets_returns403() throws Exception {
        List<Long> ticketIds = List.of(99L, 100L);

        doThrow(new AccessDeniedException("Access denied"))
            .when(reservationService).cancelReservations(ticketIds, "user@test.com");

        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isForbidden()); // prüft korrekt 403
    }


    @Test
    void reserve_noTicketsFound_throwsNotFoundException() throws Exception {
        List<Long> ticketIds = List.of();

        when(reservationService.create(ticketIds, "user@test.com"))
            .thenThrow(new NotFoundException("No tickets found for provided IDs"));

        mockMvc.perform(patch("/api/v1/reservations")
                .principal(new TestingAuthenticationToken("user@test.com", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isNotFound());
    }


    // ---------------- getMyReservations ----------------
    @Test
    void getMyReservations_noReservations_returnsEmptyList() throws Exception {
        when(reservationService.findByUser("user@test.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reservations/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }


}
