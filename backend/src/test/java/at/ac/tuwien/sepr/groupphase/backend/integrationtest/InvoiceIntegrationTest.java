package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private SectorRepository sectorRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoiceService invoiceService;

    private String userToken;
    private User testUser;
    private Long ticket1Id;
    private Long ticket2Id;
    @Autowired
    private TicketService ticketService;

    @BeforeEach
    void setup() throws Exception {
        registerUser("user@test.com", "password123", "First", "Last");
        userToken = login("user@test.com", "password123");

        testUser = userRepository.findByEmail("user@test.com").orElseThrow();

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

        Event event = new Event();
        event.setTitle("TestEvent");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setLocation(location);
        eventRepository.save(event);

        ticket1Id = createTicket(seat1, event);
        ticket2Id = createTicket(seat2, event);
    }

    @AfterEach
    void cleanup() {
        invoiceRepository.deleteAll();
        ticketRepository.deleteAll();
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


    private String bearer(String token) {
        System.out.println(token);
        return token;
    }

    // ---------------------------------------------------------
    // TESTS
    // ---------------------------------------------------------
    @Test
    void createInvoice_withTickets_success() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id, ticket2Id)
        );

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.invoiceNumber").isNotEmpty());

        var savedInvoice = invoiceRepository.findAll().get(0);
        assertEquals(2, savedInvoice.getTickets().size(), "Invoice should contain 2 tickets");
        assertEquals(testUser.getId(), savedInvoice.getUser().getId());
    }

    @Test
    void createInvoice_userNotFound_returnsNotFound() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            999L, "fn", "ln", "addr", LocalDateTime.now(), List.of(ticket1Id)
        );

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void createInvoice_ticketNotFound_returnsNotFound() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(999L)
        );

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void downloadInvoicePdf_success() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id)
        );
        var invoice = mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn();

        Long invoiceId = objectMapper.readTree(invoice.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/download")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void downloadInvoicePdf_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/9999/download")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void createCreditInvoice_success() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id)
        );
        var invoice = mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn();

        Long invoiceId = objectMapper.readTree(invoice.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void downloadCreditInvoice_success() throws Exception {

        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id)
        );

        var invoiceResponse = mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn();

        var creditResponse = mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticket1Id))))
            .andExpect(status().isOk())
            .andReturn();

        Long creditInvoiceId = invoiceRepository.findAll().stream()
            .filter(i -> i.getOriginalInvoiceNumber() != null)
            .findFirst()
            .orElseThrow()
            .getId();


        mockMvc.perform(get("/api/v1/invoices/credit/" + creditInvoiceId + "/download")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void downloadCreditInvoice_otherUser_forbidden() throws Exception {
        InvoiceCreateDto regularInvoiceDto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id)
        );
        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regularInvoiceDto)))
            .andExpect(status().isCreated());
        List<Long> ticketIds = List.of(ticket1Id);
        byte[] pdf = mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketIds)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        Long creditInvoiceId = invoiceRepository.findAll().stream()
            .filter(i -> i.getOriginalInvoiceNumber() != null)
            .findFirst()
            .orElseThrow()
            .getId();

        registerUser("hacker@test.com", "password123", "First", "Last");
        String hackerToken = login("hacker@test.com", "password123");

        mockMvc.perform(get("/api/v1/invoices/credit/" + creditInvoiceId + "/download")
                .header("Authorization", bearer(hackerToken)))
            .andExpect(status().isForbidden());
    }


    @Test
    void createCreditInvoice_ticketNotFound_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(999L))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCreditInvoice_emptyList_returnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createCreditInvoice_ticketNotOwned_returnsForbidden() throws Exception {

        registerUser("owner@test.com", "password123", "Other", "User");
        User owner = userRepository.findByEmail("owner@test.com").orElseThrow();

        Event event = new Event();
        event.setTitle("ForeignEvent");
        event.setDateTime(LocalDateTime.now().plusDays(2));
        event.setLocation(locationRepository.findAll().get(0));
        event = eventRepository.save(event);

        Sector sector = sectorRepository.findAll().get(0);
        Seat foreignSeat = new Seat();
        foreignSeat.setRowNumber(2);
        foreignSeat.setSeatNumber(1);
        foreignSeat.setSector(sector);
        seatRepository.save(foreignSeat);

        Ticket foreignTicket = new Ticket(foreignSeat, event);
        foreignTicket.setNetPrice(40.0);
        foreignTicket.setTaxRate(20.0);
        foreignTicket.setGrossPrice(48.0);
        foreignTicket = ticketRepository.save(foreignTicket);

        List<Long> ticketIds = new ArrayList<>();
        List<Ticket> tickets = List.of(foreignTicket);
        for (Ticket t : tickets) {
            ticketIds.add(t.getId());
        }

        InvoiceCreateDto ownerInvoiceDto = new InvoiceCreateDto(
            owner.getId(),
            owner.getFirstName(),
            owner.getLastName(),
            owner.getAddress(),
            LocalDateTime.now(),
            ticketIds
        );

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(login("owner@test.com", "password123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownerInvoiceDto)))
            .andExpect(status().isCreated());


        List<Long> creditTicketIds = new ArrayList<>();
        for (Ticket t : tickets) {
            creditTicketIds.add(t.getId());
        }

        mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditTicketIds)))
            .andExpect(status().isForbidden());
    }



    @Test
    void createCreditInvoice_ticketWithoutInvoice_returnsUnprocessableEntity() throws Exception {

        Ticket uninvoicedTicket = ticketRepository.findById(ticket2Id).orElseThrow();

        mockMvc.perform(post("/api/v1/invoices/credit")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(uninvoicedTicket.getId()))))
            .andExpect(status().isUnprocessableEntity());
    }







    @Test
    void downloadInvoice_userForbidden_returnsForbidden() throws Exception {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            testUser.getId(),
            testUser.getFirstName(),
            testUser.getLastName(),
            testUser.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1Id)
        );

        var invoiceResult = mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andReturn();

        Long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString())
            .get("id").asLong();

        registerUser("hacker@test.com", "password123", "Hacker", "User");
        String hackerToken = login("hacker@test.com", "password123");

        mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/download")
                .header("Authorization", bearer(hackerToken)))
            .andExpect(status().isForbidden());
    }



    @Test
    void getMyInvoices_returnsList() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/my")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyMerchandiseInvoices_returnsList() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/my/merchandise")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

}
