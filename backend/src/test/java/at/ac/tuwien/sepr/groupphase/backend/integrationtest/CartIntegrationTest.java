package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
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
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private MerchandiseRepository merchandiseRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TicketRepository ticketRepository;

    private String userToken;
    private User testUser;

    private Merchandise merch;
    private Merchandise rewardMerch;
    private Long ticketId;

    @BeforeEach
    void setup() throws Exception {
        registerUser("cart@test.com", "password123", "First", "Last");
        userToken = login("cart@test.com", "password123");

        testUser = userRepository.findByEmail("cart@test.com").orElseThrow();
        testUser.setAddress("Address 1");
        testUser.setTotalCentsSpent(10_000L);
        testUser.setRewardPoints(2_000);
        userRepository.save(testUser);

        Location location = new Location();
        location.setName("CartLocation");
        location.setZipCode(1010);
        location.setCity("Vienna");
        location.setStreet("Teststreet");
        location.setStreetNumber("1");
        location.setStagePosition("TOP");
        locationRepository.save(location);

        Sector sector = new Sector();
        sector.setName("A");
        sector.setLocation(location);
        sectorRepository.save(sector);

        Seat seat = new Seat();
        seat.setRowNumber(1);
        seat.setSeatNumber(1);
        seat.setSector(sector);
        seatRepository.save(seat);

        Event event = new Event();
        event.setTitle("CartEvent");
        event.setDateTime(LocalDateTime.now().plusDays(1));
        event.setLocation(location);
        eventRepository.save(event);

        Reservation reservation = new Reservation();
        reservation.setEvent(event);
        reservation.setUser(testUser);
        reservationRepository.save(reservation);

        Ticket ticket = new Ticket(seat, event);
        ticket.setReservation(reservation);

        ticket.setGrossPrice(60.0);
        ticket.setTaxRate(0.20);
        ticket.setNetPrice(50.0);

        ticketRepository.save(ticket);
        ticketId = ticket.getId();

        merch = new Merchandise();
        merch.setName("T-Shirt");
        merch.setDescription("Normal merch");
        merch.setUnitPrice(new BigDecimal("10.00"));
        merch.setRewardPointsPerUnit(50);
        merch.setRemainingQuantity(100);
        merch.setRedeemableWithPoints(false);
        merch.setDeleted(false);
        merchandiseRepository.save(merch);

        rewardMerch = new Merchandise();
        rewardMerch.setName("Reward Mug");
        rewardMerch.setDescription("Redeemable");
        rewardMerch.setUnitPrice(new BigDecimal("5.00"));
        rewardMerch.setRewardPointsPerUnit(10);
        rewardMerch.setRemainingQuantity(50);
        rewardMerch.setRedeemableWithPoints(true);
        rewardMerch.setPointsPrice(250);
        rewardMerch.setDeleted(false);
        merchandiseRepository.save(rewardMerch);
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
        return mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
                        .withEmail(email)
                        .withPassword(pw)
                        .build()
                )))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private String bearer(String token) {
        return token;
    }

    private Map<String, Object> validCheckoutRequest() {
        return Map.of(
            "paymentMethod", PaymentMethod.CREDIT_CARD.name(),
            "paymentDetail", Map.of(
                "cardNumber", "4242424242424242",
                "expiryMonthYear", "0129",
                "cvc", "123"
            )
        );
    }

    // ---------------------------------------------------------
    // SECURITY
    // ---------------------------------------------------------

    @Test
    void getCart_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // /tickets input binding vs endpoint guard
    // ---------------------------------------------------------

    @Test
    void addTickets_withEmptyList_resultsInIllegalArgumentException() throws Exception {
        mockMvc.perform(post("/api/v1/cart/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))

            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Ticket IDs dürfen nicht leer sein")));
    }

    @Test
    void addTickets_withValidList_returns200_andResponseContainsId() throws Exception {
        mockMvc.perform(post("/api/v1/cart/tickets")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(ticketId))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").isNumber());
    }

    // ---------------------------------------------------------
    // MERCH purchase
    // ---------------------------------------------------------

    @Test
    void checkout_merchandisePurchase_createsInvoice_updatesStock_andClearsCart() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "merchandiseId", merch.getId(),
                        "quantity", 3,
                        "redeemedWithPoints", false
                    )
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].merchandiseId").value(merch.getId()))
            .andExpect(jsonPath("$.items[0].quantity").value(3));

        String response = mockMvc.perform(post("/api/v1/cart/checkout")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCheckoutRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.merchandiseInvoiceId").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        long invoiceId = json.get("merchandiseInvoiceId").asLong();

        assertThat(invoiceRepository.findById(invoiceId)).isPresent();

        Merchandise updated = merchandiseRepository.findById(merch.getId()).orElseThrow();
        assertThat(updated.getRemainingQuantity()).isEqualTo(97);

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(cartItemRepository.findAllByCartId(cart.getId())).isEmpty();
    }

    @Test
    void addItem_merchandiseInsufficientStock_returns422_andDoesNotChangeStock() throws Exception {
        int before = merchandiseRepository.findById(merch.getId()).orElseThrow().getRemainingQuantity();

        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "merchandiseId", merch.getId(),
                        "quantity", 9999,
                        "redeemedWithPoints", false
                    )
                )))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.message").value(containsString("exceeds remaining quantity")));

        Merchandise after = merchandiseRepository.findById(merch.getId()).orElseThrow();
        assertThat(after.getRemainingQuantity()).isEqualTo(before);
    }

    // ---------------------------------------------------------
    // REWARD purchase
    // ---------------------------------------------------------

    @Test
    void checkout_rewardPurchase_deductsPoints_updatesStock_andClearsCart() throws Exception {
        int pointsBefore = userRepository.findById(testUser.getId()).orElseThrow().getRewardPoints();
        int stockBefore = merchandiseRepository.findById(rewardMerch.getId()).orElseThrow().getRemainingQuantity();

        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "merchandiseId", rewardMerch.getId(),
                        "quantity", 2,
                        "redeemedWithPoints", true
                    )
                )))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/checkout")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCheckoutRequest())))
            .andExpect(status().isCreated());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getRewardPoints()).isEqualTo(pointsBefore - 500);

        Merchandise updatedRewardMerch = merchandiseRepository.findById(rewardMerch.getId()).orElseThrow();
        assertThat(updatedRewardMerch.getRemainingQuantity()).isEqualTo(stockBefore - 2);

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(cartItemRepository.findAllByCartId(cart.getId())).isEmpty();
    }

    @Test
    void rewardPurchase_notRegularCustomer_returns422_andDoesNotDeductPoints() throws Exception {
        testUser.setTotalCentsSpent(0L);
        userRepository.save(testUser);

        int pointsBefore = userRepository.findById(testUser.getId()).orElseThrow().getRewardPoints();

        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "merchandiseId", rewardMerch.getId(),
                        "quantity", 1,
                        "redeemedWithPoints", true
                    )
                )))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.message").value(containsString("not a regular customer")));

        User after = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(after.getRewardPoints()).isEqualTo(pointsBefore);
    }

    @Test
    void rewardPurchase_insufficientPoints_returns422_andDoesNotChangeStock() throws Exception {
        testUser.setRewardPoints(0);
        userRepository.save(testUser);

        int stockBefore = merchandiseRepository.findById(rewardMerch.getId()).orElseThrow().getRemainingQuantity();

        mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of(
                        "merchandiseId", rewardMerch.getId(),
                        "quantity", 1,
                        "redeemedWithPoints", true
                    )
                )))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.message").value(containsString("Insufficient reward points")));

        Merchandise after = merchandiseRepository.findById(rewardMerch.getId()).orElseThrow();
        assertThat(after.getRemainingQuantity()).isEqualTo(stockBefore);
    }


}
