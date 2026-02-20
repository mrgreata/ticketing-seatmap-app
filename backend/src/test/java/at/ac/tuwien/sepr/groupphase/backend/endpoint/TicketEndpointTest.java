package at.ac.tuwien.sepr.groupphase.backend.endpoint;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.TicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = TicketEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class TicketEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
     private TicketMapper ticketMapper;


    // ---------- CREATE ----------

    @Test
    void createTickets_returns201() throws Exception {
        DetailedTicketDto dto = testDto();

        when(ticketService.create(any(), any())).thenReturn(List.of(dto));

        TicketCreateDto createDto = new TicketCreateDto(30L, 20L);

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(createDto)))
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].eventName").value("SEPR Konzert"))
            .andExpect(jsonPath("$[0].price").value(60.0));

    }
    @Test
    void createTickets_userNotFound_throws404() throws Exception {
        TicketCreateDto createDto = new TicketCreateDto(30L, 20L);
        when(ticketService.create(anyList(), anyString()))
            .thenThrow(new NotFoundException("User not found: user@test.com"));

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(createDto)))
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isNotFound());
    }

    // ---------- GET MY TICKETS ----------

    @Test
    void getMyTickets_returns200() throws Exception {
        when(ticketService.getMyPurchasedTickets(any()))
            .thenReturn(List.of(testDto()));

        mockMvc.perform(get("/api/v1/tickets/my")
                .principal(new TestingAuthenticationToken("user@test.at", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventName").value("SEPR Konzert"));
    }

    @Test
    void getMyTickets_noTickets_returnsEmptyList() throws Exception {
        when(ticketService.getMyPurchasedTickets(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tickets/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- GET BY ID ----------

    @Test
    void findById_returns200() throws Exception {
        Ticket ticket = new Ticket();
        when(ticketService.getTicketByIdForUser(eq(1L), eq("user@test.at")))
            .thenReturn(testDto());
        when(ticketMapper.toDetailed(ticket)).thenReturn(testDto());

        mockMvc.perform(get("/api/v1/tickets/1")
            .principal(new TestingAuthenticationToken("user@test.at", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventName").value("SEPR Konzert"));
    }

    @Test
    void findById_notFound_throws404() throws Exception {
        when(ticketService.getTicketByIdForUser(99L, "user@test.com"))
            .thenThrow(new NotFoundException("Ticket not found: 99"));

        mockMvc.perform(get("/api/v1/tickets/99")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isNotFound());
    }

    @Test
    void findById_accessDenied_returns403() throws Exception {
        when(ticketService.getTicketByIdForUser(1L, "user@test.com"))
            .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/tickets/1")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden());
    }

    // ---------- PURCHASE ----------

    @Test
    void purchaseTickets_returns200() throws Exception {
        when(ticketService.purchase(anyList(), anyString()))
            .thenReturn(List.of(testDto()));

        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L)))
            .principal(new TestingAuthenticationToken("user@test.at", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].price").value(60.0));
    }

    @Test
    void purchaseTickets_accessDenied_returns403() throws Exception {
        when(ticketService.purchase(anyList(), anyString()))
            .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L)))
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden());
    }


    // ---------- DELETE ----------

    @Test
    void cancelTickets_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets")
                .principal(new TestingAuthenticationToken("user@test.at", "pw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
            .andExpect(status().isNoContent());
    }

    @Test
    void cancelTickets_accessDenied_returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied"))
            .when(ticketService).createCreditInvoice(anyList(), anyString());

        mockMvc.perform(delete("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L)))
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isForbidden());
    }

    // ---------- CANCELLED TICKETS ----------

    @Test
    void getMyCancelledTickets_returns200() throws Exception {
        CancelledTicketDto dto = new CancelledTicketDto(
            1L,
            "Concert X",
            LocalDateTime.of(2026, 1, 25, 18, 0),
            LocalDate.of(2026, 1, 24),
            List.of("A1", "A2"),
            42L,
            true
        );

        when(ticketService.getMyCancelledTickets(anyString()))
            .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/tickets/cancelled/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].eventName").value("Concert X"))
            .andExpect(jsonPath("$[0].eventDate").value("2026-01-25T18:00:00"))
            .andExpect(jsonPath("$[0].cancellationDate").value("2026-01-24"))
            .andExpect(jsonPath("$[0].seats[0]").value("A1"))
            .andExpect(jsonPath("$[0].seats[1]").value("A2"))
            .andExpect(jsonPath("$[0].creditInvoiceId").value(42L))
            .andExpect(jsonPath("$[0].selected").value(true));
    }

    @Test
    void getMyCancelledTickets_userNotFound_returns404() throws Exception {
        when(ticketService.getMyCancelledTickets(anyString()))
            .thenThrow(new NotFoundException("User not found: user@test.com"));

        mockMvc.perform(get("/api/v1/tickets/cancelled/my")
                .principal(new TestingAuthenticationToken("user@test.com", "pw")))
            .andExpect(status().isNotFound());
    }





    // ---------- Test Helper ----------

    private DetailedTicketDto testDto() {
        return new DetailedTicketDto(
            1L,
            "SEPR Konzert",
            1,
            2,
            20L,
            null,
            "10:30",
            "INV-1",
            null,
            60.0,
            "Wien",
            false,
            30L,
            5L,
            null,
            1L
        );
    }
}

