package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private LocationRepository locationRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private InvoiceRepository invoiceRepository;



    private String userToken;
    private String otherUserToken;
    private User testUser;
    private User otherUser;
    private Long ticket1Id;
    private Long ticket2Id;
    private Long ticket3Id;


    @BeforeEach
    void setup() throws Exception {

        registerUser("user@test.com", "password123", "First", "Last");
        registerUser("other@test.com", "password123", "Other", "User");

        userToken = login("user@test.com", "password123");
        otherUserToken = login("other@test.com", "password123");

        testUser = userRepository.findByEmail("user@test.com").orElseThrow();
        otherUser = userRepository.findByEmail("other@test.com").orElseThrow();

        Location location = new Location();
        location.setName("LocationTest");
        location.setZipCode(1010);
        location.setCity("Vienna");
        location.setStreet("StreetTest");
        location.setStreetNumber("1");
        location.setStagePosition("TOP");
        locationRepository.save(location);

        Sector sector = new Sector();
        sector.setName("SectorTest");
        sector.setLocation(location);
        sectorRepository.save(sector);

        Seat seat1 = new Seat();
        seat1.setRowNumber(1);
        seat1.setSeatNumber(1);
        seat1.setSector(sector);
        seatRepository.save(seat1);

        Seat seat2 = new Seat();
        seat2.setRowNumber(1);
        seat2.setSeatNumber(2);
        seat2.setSector(sector);
        seatRepository.save(seat2);

        Seat seat3 = new Seat();
        seat3.setRowNumber(1);
        seat3.setSeatNumber(3);
        seat3.setSector(sector);
        seatRepository.save(seat3);


        Event event = new Event();
        event.setTitle("TestEvent");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setLocation(location);
        eventRepository.save(event);

        ticket1Id = createTicket(seat1, event);
        ticket2Id = createTicket(seat2, event);
        ticket3Id = createTicket(seat3, event);

        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        Cart cart = new Cart(user);
        cartRepository.save(cart);
    }

    @AfterEach
    void cleanup() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        invoiceRepository.deleteAll();

        ticketRepository.findAll().forEach(ticket -> {
            ticket.setInvoice(null);
            ticket.setReservation(null);
            ticketRepository.save(ticket);
        });
        ticketRepository.deleteAll();

        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        sectorRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------
    private void registerUser(String email, String pw, String fn, String ln) throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserRegisterDto(email, pw, fn, ln)
                )))
            .andExpect(status().isCreated());
    }

    private String login(String email, String pw) throws Exception {
        var result = mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
                        .withEmail(email)
                        .withPassword(pw)
                        .build()
                )))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getContentAsString();
    }

    private Long createTicket(Seat seat, Event event) {
        Ticket ticket = new Ticket(seat, event);
        ticket.setNetPrice(50.0);
        ticket.setTaxRate(20.0);
        ticket.setGrossPrice(60.0);
        ticketRepository.save(ticket);
        return ticket.getId();
    }
    private Long createReservation(List<Long> ticketIds) throws Exception {
        var result = mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("id").asLong();
    }

    private String bearer(String token) {
        return token;
    }

    // ---------------------------------------------------------
    // TESTS
    // ---------------------------------------------------------
    @Test
    void reserveTickets_success() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.reservationNumber").isNotEmpty())
            .andExpect(jsonPath("$.userId").value(testUser.getId()));

        var reservation = reservationRepository.findAll().get(0);
        assertEquals(2, reservation.getTickets().size());
        assertEquals(testUser.getId(), reservation.getUser().getId());
    }

    @Test
    void reserveTickets_emptyList_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReservations_success() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reservations/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].ticketId").isNumber())
            .andExpect(jsonPath("$[1].ticketId").isNumber());
    }

    @Test
    void getMyReservations_noReservations_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getReservationById_success() throws Exception {
        Long reservationId = createReservation(List.of(ticket1Id));

        mockMvc.perform(get("/api/v1/reservations/" + reservationId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reservationId))
            .andExpect(jsonPath("$.reservationNumber").isNotEmpty())
            .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    void getReservationById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/999")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void getReservationById_otherUsersReservation_returnsForbidden() throws Exception {
        Long reservationId = createReservation(List.of(ticket1Id));

        mockMvc.perform(get("/api/v1/reservations/" + reservationId)
                .header("Authorization", bearer(otherUserToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancelReservation_success() throws Exception {

        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isCreated());

        Ticket ticket1 = ticketRepository.findById(ticket1Id).orElseThrow();
        Ticket ticket2 = ticketRepository.findById(ticket2Id).orElseThrow();
        assertNotNull(ticket1.getReservation(), "Ticket1 should be reserved");
        assertNotNull(ticket2.getReservation(), "Ticket2 should be reserved");

        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isOk());

        assertEquals(0, reservationRepository.count(), "No reservations should remain");

        ticket1 = ticketRepository.findById(ticket1Id).orElse(null);
        ticket2 = ticketRepository.findById(ticket2Id).orElse(null);


        assertNull(ticket1, "Ticket1 should be deleted after cancellation");
        assertNull(ticket2, "Ticket2 should be deleted after cancellation");

    }

    @Test
    void cancelReservation_partialCancellation_success() throws Exception {
        createReservation(List.of(ticket1Id, ticket2Id, ticket3Id));


        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isOk());

        assertEquals(1, reservationRepository.count());
        Reservation reservation = reservationRepository.findAll().get(0);
        assertEquals(1, reservation.getTickets().size());
        assertTrue(reservation.getTickets().stream()
            .anyMatch(t -> t.getId().equals(ticket3Id)));

        assertNull(ticketRepository.findById(ticket1Id).orElse(null));
        assertNull(ticketRepository.findById(ticket2Id).orElse(null));

        assertNotNull(ticketRepository.findById(ticket3Id).orElse(null));
    }

    @Test
    void cancelReservation_ticketNotReserved_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isNotFound());
    }


    @Test
    void reserveTickets_ticketNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void reserveTickets_ticketAlreadyReserved_returnsUnprocessableEntity() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reserveTickets_ticketAlreadyPurchased_returnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto(
                        testUser.getId(),
                        testUser.getFirstName(),
                        testUser.getLastName(),
                        testUser.getAddress(),
                        LocalDateTime.now(),
                        List.of(ticket1Id)
                    ))))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelReservation_otherUsersReservation_returnsForbidden() throws Exception {
        createReservation(List.of(ticket1Id));

        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancelReservation_emptyList_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cancelReservation_ticketNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelReservation_mixedValidAndInvalidTickets_returnsError() throws Exception {
        createReservation(List.of(ticket1Id));

        mockMvc.perform(patch("/api/v1/reservations/cancellation")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id, ticket2Id))))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteReservation_success() throws Exception {
        Long reservationId = createReservation(List.of(ticket1Id));

        assertEquals(1, reservationRepository.count());

        assertNotNull(ticketRepository.findById(ticket1Id).orElse(null));
    }
}
