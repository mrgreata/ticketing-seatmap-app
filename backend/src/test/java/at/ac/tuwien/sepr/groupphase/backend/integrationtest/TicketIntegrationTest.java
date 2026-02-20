package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PriceCategoryRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SectorRepository sectorRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private PriceCategoryRepository priceCategoryRepository;
    @Autowired
    private UserRepository userRepository;

    private Long eventId;
    private Long seat1Id;
    private Long seat2Id;
    private Long seat3Id;
    private Long seat4Id;
    private String userToken;
    private String otherUserToken;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setup() throws Exception {
        register("user@test.com", "password123", "User", "Test");
        register("other@test.com", "password123", "Other", "User");

        userToken = login("user@test.com", "password123");
        otherUserToken = login("other@test.com", "password123");

        testUser = userRepository.findByEmail("user@test.com").orElseThrow();
        otherUser = userRepository.findByEmail("other@test.com").orElseThrow();

        Location location = new Location();
        location.setName("LocationTest");
        location.setZipCode(1);
        location.setCity("LocationCityTest");
        location.setStreet("LocationStreetTest");
        location.setStreetNumber("1");
        location.setStagePosition("TOP");
        locationRepository.save(location);

        Sector sector = new Sector();
        sector.setName("SectorTest");
        sector.setLocation(location);
        sectorRepository.save(sector);

        PriceCategory priceCategory = new PriceCategory();
        priceCategory.setDescription("PriceCategoryTest");
        priceCategory.setBasePrice(5000);
        priceCategory.setSector(sector);
        priceCategoryRepository.save(priceCategory);

        Seat seat1 = new Seat();
        seat1.setRowNumber(1);
        seat1.setSeatNumber(1);
        seat1.setSector(sector);
        seat1.setPriceCategory(priceCategory);
        seat1 = seatRepository.save(seat1);
        seat1Id = seat1.getId();

        Seat seat2 = new Seat();
        seat2.setRowNumber(2);
        seat2.setSeatNumber(2);
        seat2.setSector(sector);
        seat2.setPriceCategory(priceCategory);
        seat2 = seatRepository.save(seat2);
        seat2Id = seat2.getId();

        Seat seat3 = new Seat();
        seat3.setRowNumber(3);
        seat3.setSeatNumber(3);
        seat3.setSector(sector);
        seat3.setPriceCategory(priceCategory);
        seat3 = seatRepository.save(seat3);
        seat3Id = seat3.getId();

        Seat seat4 = new Seat();
        seat4.setRowNumber(4);
        seat4.setSeatNumber(4);
        seat4.setSector(sector);
        seat4.setPriceCategory(priceCategory);
        seat4 = seatRepository.save(seat4);
        seat4Id = seat4.getId();

        Event event = new Event();
        event.setTitle("EventTitleTest");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setLocation(location);
        event = eventRepository.save(event);
        eventId = event.getId();
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private void register(String email, String pw, String fn, String ln) throws Exception {
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

    private String bearer(String token) {
        System.out.println(token);
        return  token;
    }

    private Long createOneTicket() throws Exception {
        return createOneTicket(eventId, seat1Id);
    }

    private Long createOneTicket(Long eventId, Long seatId) throws Exception {
        List<TicketCreateDto> tickets = List.of(new TicketCreateDto(eventId, seatId));

        var result = mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tickets)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get(0).get("id").asLong();
    }

    private Long purchaseTicket(Long ticketId) throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isOk());

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        return ticket.getInvoice().getId();
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    void createTickets_asUser_returnsCreatedTickets() throws Exception {
        List<TicketCreateDto> tickets = List.of(
            new TicketCreateDto(eventId, seat1Id),
            new TicketCreateDto(eventId, seat2Id)
        );

        var result = mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tickets)))
            .andExpect(status().isCreated())
            // 1️⃣ Prüfe Anzahl der Tickets
            .andExpect(jsonPath("$", hasSize(2)))
            // 2️⃣ Prüfe, dass jedes Ticket eine ID hat
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[1].id").isNumber())
            // 3️⃣ Prüfe Preise
            .andExpect(jsonPath("$[0].price").value(60.0))
            .andExpect(jsonPath("$[1].price").value(60.0))
            // 4️⃣ Prüfe Event-Zuordnung
            .andExpect(jsonPath("$[0].eventId").value(eventId))
            .andExpect(jsonPath("$[1].eventId").value(eventId))
            // 5️⃣ Prüfe Seat-Zuordnung
            .andExpect(jsonPath("$[0].seatId").value(seat1Id))
            .andExpect(jsonPath("$[1].seatId").value(seat2Id))
            // 6️⃣ Optional: Prüfe, dass Invoice oder User null ist, wenn noch nicht gekauft
            .andExpect(jsonPath("$[0].invoiceId").doesNotExist())
            .andExpect(jsonPath("$[1].invoiceId").doesNotExist())
            .andReturn();

        // Optional: Mappe Response auf Objekte, um noch weitere Assertions durchzuführen
        var ticketArray = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, ticketArray.size(), "Should return 2 tickets");

        // Prüfe Title, Event-Zeit oder andere Felder
        for (int i = 0; i < ticketArray.size(); i++) {
            var ticketNode = ticketArray.get(i);
            assertTrue(ticketNode.get("id").asLong() > 0, "Ticket ID should be positive");
            assertEquals(60.0, ticketNode.get("price").asDouble(), 0.01);
            assertEquals(eventId, ticketNode.get("eventId").asLong());
        }
    }
    @Test
    void createTickets_emptyList_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andExpect(status().isBadRequest());
    }


    @Test
    void createTickets_invalidEventId_returnsNotFound() throws Exception {
        List<TicketCreateDto> tickets = List.of(new TicketCreateDto(999L, seat1Id));

        mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tickets)))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTickets_invalidSeatId_returnsNotFound() throws Exception {
        List<TicketCreateDto> tickets = List.of(new TicketCreateDto(eventId, 999L));

        mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tickets)))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTickets_sameSeatTwice_returnsUnprocessableEntity() throws Exception {
        List<TicketCreateDto> tickets = List.of(
            new TicketCreateDto(eventId, seat1Id),
            new TicketCreateDto(eventId, seat1Id) // Same seat!
        );

        mockMvc.perform(post("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tickets)))
            .andExpect(status().isUnprocessableEntity());
    }



    // ---------------------------------------------------------
    // PURCHASE
    // ---------------------------------------------------------

    @Test
    void purchaseTickets_successfully() throws Exception {
        Long ticketId = createOneTicket();

        var result = mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ticketId))
            .andExpect(jsonPath("$[0].invoiceId").isNumber())
            .andExpect(jsonPath("$[0].price").value(60.0))
            .andExpect(jsonPath("$[0].eventId").value(eventId))
            .andExpect(jsonPath("$[0].seatId").value(seat1Id))
            .andReturn();

        var ticketNode = objectMapper.readTree(result.getResponse().getContentAsString()).get(0);
        assertTrue(ticketNode.get("invoiceId").asLong() > 0, "Invoice ID should be positive");
        assertEquals(60.0, ticketNode.get("price").asDouble(), 0.01);
        assertEquals(eventId, ticketNode.get("eventId").asLong());
    }

    @Test
    void purchaseTickets_emptyList_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void purchaseTickets_ticketNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
            .andExpect(status().isNotFound());
    }

    @Test
    void purchaseTickets_alreadyPurchased_returnsUnprocessableEntity() throws Exception {
        Long ticketId = createOneTicket();
        purchaseTicket(ticketId);

        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isUnprocessableEntity());
    }


    @Test
    void purchaseTickets_otherUsersReservedTicket_returnsForbidden() throws Exception {
        Long ticketId = createOneTicket();

        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isForbidden());
    }





    // ---------------------------------------------------------
    // GET MY TICKETS
    // ---------------------------------------------------------

    @Test
    void getMyTickets_afterPurchase_returnsTickets() throws Exception {
        Long ticketId = createOneTicket();

        // Kaufe Ticket
        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isOk());

        var result = mockMvc.perform(get("/api/v1/tickets/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ticketId))
            .andExpect(jsonPath("$[0].invoiceId").isNumber())
            .andExpect(jsonPath("$[0].price").value(60.0))
            .andExpect(jsonPath("$[0].eventId").value(eventId))
            .andExpect(jsonPath("$[0].seatId").value(seat1Id))
            .andReturn();

        var ticketNode = objectMapper.readTree(result.getResponse().getContentAsString()).get(0);
        assertTrue(ticketNode.get("invoiceId").asLong() > 0, "Invoice ID should be positive");
        assertEquals(60.0, ticketNode.get("price").asDouble(), 0.01);
    }

    @Test
    void getMyTickets_noPurchases_returnsEmptyList() throws Exception {
        createOneTicket(); // Not purchased

        mockMvc.perform(get("/api/v1/tickets/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
    @Test
    void getMyTickets_onlyShowsOwnTickets() throws Exception {
        Long ticketId1 = createOneTicket(eventId, seat1Id);
        purchaseTicket(ticketId1);

        Long ticketId2 = createOneTicket(eventId, seat2Id);
        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId2))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tickets/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ticketId1));
    }

    // ---------------------------------------------------------
    // FIND BY ID
    // ---------------------------------------------------------

    @Test
    void findTicketById() throws Exception {
        Long ticketId = createOneTicket();

        var result = mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ticketId))
            .andExpect(jsonPath("$.eventId").value(eventId))
            .andExpect(jsonPath("$.seatId").value(seat1Id))
            .andReturn();

        var ticketNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(ticketId, ticketNode.get("id").asLong());
        assertEquals(eventId, ticketNode.get("eventId").asLong());
    }

    @Test
    void findTicketById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/999")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void findTicketById_otherUsersTicket_returnsForbidden() throws Exception {
        Long ticketId = createOneTicket();
        purchaseTicket(ticketId);

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                .header("Authorization", bearer(otherUserToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void findTicketById_reservedByOtherUser_returnsForbidden() throws Exception {
        Long ticketId = createOneTicket();

        mockMvc.perform(patch("/api/v1/reservations")
                .header("Authorization", bearer(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Test
    void cancelTickets_afterPurchase_successfully() throws Exception {
        Long ticketId = createOneTicket();

        mockMvc.perform(patch("/api/v1/tickets/purchasing")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].invoiceId").isNumber());

        mockMvc.perform(delete("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isNoContent());

        assertTrue(ticketRepository.findById(ticketId).isEmpty(), "Ticket should be deleted after purchase");
    }

    @Test
    void cancelTickets_ticketNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
            .andExpect(status().isNotFound());
    }


    // ---------------------------------------------------------
    // SECURITY
    // ---------------------------------------------------------

    @Test
    void accessWithoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/my"))
            .andExpect(status().isForbidden());
    }


}
