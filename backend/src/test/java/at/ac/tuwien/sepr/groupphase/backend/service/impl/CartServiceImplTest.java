package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutResultDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.MerchandisePurchaseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import at.ac.tuwien.sepr.groupphase.backend.entity.CartItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserService userService;
    @Mock private MerchandiseRepository merchandiseRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private TicketService ticketService;
    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;

    private CartServiceImpl cartService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setup() {
        cartService = new CartServiceImpl(
            cartRepository,
            cartItemRepository,
            userService,
            merchandiseRepository,
            invoiceService,
            ticketService,
            reservationRepository,
            ticketRepository,
            userRepository
        );

        user = new User();
        user.setId(10L);
        user.setEmail("user@test.at");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setAddress("Address");
        user.setRewardPoints(1000);
        user.setTotalCentsSpent(10_000L);

        cart = new Cart(user);
        cart.setId(100L);
        cart.setUser(user);
    }

    private PaymentDetailDto validCard() {
        return new PaymentDetailDto("4242424242424242", "0129", "123", null);
    }

    private Merchandise merch(long id, String name, String price, int remaining) {
        Merchandise m = new Merchandise();
        m.setId(id);
        m.setName(name);
        m.setUnitPrice(new BigDecimal(price));
        m.setRemainingQuantity(remaining);
        return m;
    }

    private Merchandise redeemableMerch(long id, String name, int pointsPrice, int remaining) {
        Merchandise m = merch(id, name, "10.00", remaining);
        m.setRedeemableWithPoints(true);
        m.setPointsPrice(pointsPrice);
        return m;
    }

    private CartItem merchItem(long cartItemId, Cart cart, CartItemType type, Merchandise m, int quantity) {
        CartItem ci = new CartItem(type);
        ci.setId(cartItemId);
        ci.setCart(cart);
        ci.setMerchandise(m);
        ci.setQuantity(quantity);
        ci.setTicket(null);
        return ci;
    }

    private CartItem ticketItem(long cartItemId, Cart cart, Ticket ticket) {
        CartItem ci = new CartItem(CartItemType.TICKET);
        ci.setId(cartItemId);
        ci.setCart(cart);
        ci.setTicket(ticket);
        ci.setMerchandise(null);
        ci.setQuantity(null);
        return ci;
    }



    // -------------------------
    // getMyCart
    // -------------------------


    @Test
    void getMyCart_cartExists_returnsDto() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of());

        CartDto dto = cartService.getMyCart(user.getEmail());

        assertAll(
            () -> assertThat(dto).isNotNull(),
            () -> assertThat(dto.id()).isEqualTo(cart.getId()),
            () -> assertThat(dto.items()).isEmpty(),
            () -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO)
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getMyCart_cartMissing_createsAndReturnsDto() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of());

        CartDto dto = cartService.getMyCart(user.getEmail());

        assertAll(
            () -> assertThat(dto).isNotNull(),
            () -> assertThat(dto.id()).isEqualTo(200L),
            () -> assertThat(dto.items()).isEmpty(),
            () -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO)
        );

        verify(cartRepository).save(any(Cart.class));
    }

    // -------------------------
    // addMerchandiseItem (MERCHANDISE)
    // -------------------------

    @Test
    void addMerchandiseItem_invalidArgs_throwValidation() {
        assertAll(
            () -> assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), null, 1, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("merchandiseId"),
            () -> assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 0, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Quantity"),
            () -> assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, -5, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Quantity")
        );
    }

    @Test
    void addMerchandiseItem_merchNotFound_throwsNotFound() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 99L, 1, false))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(merchandiseRepository, never()).save(any(Merchandise.class));
    }

    @Test
    void addMerchandiseItem_insufficientRemaining_throwsValidation_andDoesNotPersist() {
        Merchandise m = merch(1L, "M1", "10.00", 1);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 2, false))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("exceeds remaining quantity");

        verify(merchandiseRepository, never()).save(any(Merchandise.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addMerchandiseItem_newItem_createsAndReturnsUpdatedDto_andDecrementsRemaining() {
        Merchandise m = merch(1L, "M1", "10.00", 5);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndTypeAndMerchandiseId(cart.getId(), CartItemType.MERCHANDISE, 1L))
            .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

        CartItem saved = merchItem(500L, cart, CartItemType.MERCHANDISE, m, 2);
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(saved));

        CartDto dto = cartService.addMerchandiseItem(user.getEmail(), 1L, 2, false);

        assertAll(
            () -> assertThat(dto.items()).hasSize(1),
            () -> assertThat(dto.items().get(0).type()).isEqualTo(CartItemType.MERCHANDISE),
            () -> assertThat(dto.items().get(0).merchandiseId()).isEqualTo(1L),
            () -> assertThat(dto.items().get(0).quantity()).isEqualTo(2),
            () -> assertThat(dto.total()).isEqualByComparingTo(new BigDecimal("20.00")),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(3)
        );

        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    // -------------------------
    // addMerchandiseItem (REWARD)
    // -------------------------

    @Test
    void addMerchandiseItem_reward_notRegularCustomer_throwsValidation() {
        user.setTotalCentsSpent(0L);

        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 1, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("not a regular customer");

        verify(userRepository, never()).save(any(User.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(merchandiseRepository, never()).save(any(Merchandise.class));
    }

    @Test
    void addMerchandiseItem_reward_notRedeemable_throwsValidation() {
        Merchandise m = merch(1L, "R1", "10.00", 10);
        m.setRedeemableWithPoints(false);
        m.setPointsPrice(100);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 1, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("not redeemable");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addMerchandiseItem_reward_insufficientPoints_throwsValidation() {
        user.setRewardPoints(50);

        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 1, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient reward points");

        verify(userRepository, never()).save(any(User.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(merchandiseRepository, never()).save(any(Merchandise.class));
    }

    @Test
    void addMerchandiseItem_reward_success_deductsPoints_decrementsRemaining_andDtoShowsReward() {
        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndTypeAndMerchandiseId(cart.getId(), CartItemType.REWARD, 1L))
            .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

        CartItem saved = merchItem(700L, cart, CartItemType.REWARD, m, 2);
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(saved));

        CartDto dto = cartService.addMerchandiseItem(user.getEmail(), 1L, 2, true);

        assertAll(
            () -> assertThat(user.getRewardPoints()).isEqualTo(800),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(8),
            () -> assertThat(dto.items()).hasSize(1),
            () -> assertThat(dto.items().get(0).type()).isEqualTo(CartItemType.REWARD),
            () -> assertThat(dto.items().get(0).unitPrice()).isEqualByComparingTo(BigDecimal.ZERO),
            () -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO)
        );

        verify(userRepository).save(user);
        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addMerchandiseItem_existingMerchItem_incrementsQuantity_andDecrementsRemaining() {
        Merchandise m = merch(1L, "M1", "10.00", 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem existing = merchItem(111L, cart, CartItemType.MERCHANDISE, m, 3);
        when(cartItemRepository.findByCartIdAndTypeAndMerchandiseId(cart.getId(), CartItemType.MERCHANDISE, 1L))
            .thenReturn(Optional.of(existing));

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(existing));

        CartDto dto = cartService.addMerchandiseItem(user.getEmail(), 1L, 2, false);

        assertAll(
            () -> assertThat(existing.getQuantity()).isEqualTo(5),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(8),
            () -> assertThat(dto.total()).isEqualByComparingTo(new BigDecimal("50.00"))
        );

        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(existing);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addMerchandiseItem_reward_pointsPriceMissing_throwsValidation() {
        Merchandise m = merch(1L, "R1", "10.00", 10);
        m.setRedeemableWithPoints(true);
        m.setPointsPrice(null);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 1, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Points price");

        verify(userRepository, never()).save(any(User.class));
        verify(merchandiseRepository, never()).save(any(Merchandise.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addMerchandiseItem_reward_pointsPriceNonPositive_throwsValidation() {
        Merchandise m = merch(1L, "R1", "10.00", 10);
        m.setRedeemableWithPoints(true);
        m.setPointsPrice(0);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 1, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Points price");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addMerchandiseItem_reward_costTooLarge_throwsValidation() {
        Merchandise m = redeemableMerch(1L, "R1", Integer.MAX_VALUE, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cartService.addMerchandiseItem(user.getEmail(), 1L, 2, true))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Points cost too large");

        verify(userRepository, never()).save(any(User.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // -------------------------
    // updateMerchandiseItemQuantity
    // -------------------------

    @Test
    void updateMerchandiseItemQuantity_invalidArgs_throwValidation() {
        assertAll(
            () -> assertThatThrownBy(() -> cartService.updateMerchandiseItemQuantity(user.getEmail(), null, 1))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CartItemId"),
            () -> assertThatThrownBy(() -> cartService.updateMerchandiseItemQuantity(user.getEmail(), 1L, -2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Quantity")
        );
    }

    @Test
    void updateMerchandiseItemQuantity_quantityZero_deletesItem_andRestoresRemaining_forMerch() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Merchandise m = merch(1L, "M1", "10.00", 5);
        CartItem item = merchItem(12L, cart, CartItemType.MERCHANDISE, m, 3);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of());

        CartDto dto = cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 0);

        assertAll(
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(8),
            () -> assertThat(dto.items()).isEmpty()
        );

        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).delete(item);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateMerchandiseItemQuantity_reward_quantityZero_deletesItem_restoresRemaining_refundsPoints() {
        Merchandise m = redeemableMerch(1L, "R1", 100, 5);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(12L, cart, CartItemType.REWARD, m, 3);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of());

        CartDto dto = cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 0);

        assertAll(
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(8),
            () -> assertThat(user.getRewardPoints()).isEqualTo(1300),
            () -> assertThat(dto.items()).isEmpty()
        );

        verify(merchandiseRepository).save(m);
        verify(userRepository).save(user);
        verify(cartItemRepository).delete(item);
    }

    @Test
    void updateMerchandiseItemQuantity_increaseBeyondRemaining_throwsValidation_noPersist() {
        Merchandise m = merch(1L, "M1", "10.00", 1);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(12L, cart, CartItemType.MERCHANDISE, m, 2);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 4))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("exceeds remaining quantity");

        verify(merchandiseRepository, never()).save(any(Merchandise.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateMerchandiseItemQuantity_merch_decrease_incrementsRemaining() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Merchandise m = merch(1L, "M1", "10.00", 5);
        CartItem item = merchItem(12L, cart, CartItemType.MERCHANDISE, m, 4);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(item));

        CartDto dto = cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 1);

        assertAll(
            () -> assertThat(item.getQuantity()).isEqualTo(1),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(8),
            () -> assertThat(dto.total()).isEqualByComparingTo(new BigDecimal("10.00"))
        );

        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateMerchandiseItemQuantity_reward_increase_deductsExtraPoints_andDecrementsRemaining() {
        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(12L, cart, CartItemType.REWARD, m, 2);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(item));

        int pointsBefore = user.getRewardPoints();

        CartDto dto = cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 5);

        assertAll(
            () -> assertThat(item.getQuantity()).isEqualTo(5),
            () -> assertThat(user.getRewardPoints()).isEqualTo(pointsBefore - 300),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(7),
            () -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO)
        );

        verify(userRepository, atLeastOnce()).save(user);
        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateMerchandiseItemQuantity_reward_decrease_refundsPoints_andIncrementsRemaining() {
        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(12L, cart, CartItemType.REWARD, m, 5);

        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(item));

        int pointsBefore = user.getRewardPoints();

        CartDto dto = cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 2);

        assertAll(
            () -> assertThat(item.getQuantity()).isEqualTo(2),
            () -> assertThat(user.getRewardPoints()).isEqualTo(pointsBefore + 300),
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(13),
            () -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO)
        );

        verify(userRepository, atLeastOnce()).save(user);
        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateMerchandiseItemQuantity_reward_increase_insufficientPoints_throwsValidation() {
        user.setRewardPoints(50);

        Merchandise m = redeemableMerch(1L, "R1", 100, 10);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(12L, cart, CartItemType.REWARD, m, 1);
        when(cartItemRepository.findById(12L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateMerchandiseItemQuantity(user.getEmail(), 12L, 2))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient reward points");

        verify(userRepository, never()).save(any(User.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(merchandiseRepository, never()).save(any(Merchandise.class));
    }


    // -------------------------
    // removeItem (MERCH + REWARD)
    // -------------------------

    @Test
    void removeItem_reward_refundsPoints_andRestoresRemaining() {
        Merchandise m = redeemableMerch(1L, "R1", 100, 5);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(99L, cart, CartItemType.REWARD, m, 2);
        when(cartItemRepository.findById(99L)).thenReturn(Optional.of(item));

        cartService.removeItem(user.getEmail(), 99L);

        assertAll(
            () -> assertThat(m.getRemainingQuantity()).isEqualTo(7),
            () -> assertThat(user.getRewardPoints()).isEqualTo(1200)
        );

        verify(merchandiseRepository).save(m);
        verify(userRepository).save(user);
        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_merchandise_restoresRemaining() {
        Merchandise m = merch(1L, "M1", "10.00", 5);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem item = merchItem(99L, cart, CartItemType.MERCHANDISE, m, 4);
        when(cartItemRepository.findById(99L)).thenReturn(Optional.of(item));

        cartService.removeItem(user.getEmail(), 99L);

        assertThat(m.getRemainingQuantity()).isEqualTo(9);

        verify(merchandiseRepository).save(m);
        verify(cartItemRepository).delete(item);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeItem_invalidMerchState_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartItem bad = new CartItem(CartItemType.MERCHANDISE);
        bad.setId(99L);
        bad.setCart(cart);
        bad.setMerchandise(null);
        bad.setQuantity(1);

        when(cartItemRepository.findById(99L)).thenReturn(Optional.of(bad));

        assertThatThrownBy(() -> cartService.removeItem(user.getEmail(), 99L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid cart state");

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }


    // -------------------------
    // checkout
    // -------------------------

    @Test
    void checkout_cartEmpty_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> cartService.checkout(user.getEmail(), PaymentMethod.CREDIT_CARD, validCard()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Cart is empty");
    }

    @Test
    void checkout_merchAndReward_callsPurchaseMerchandiseWithRewards_andDeletesAll() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Merchandise m1 = merch(1L, "M1", "10.00", 50);
        Merchandise r1 = redeemableMerch(2L, "R1", 100, 50);

        CartItem merchItem = merchItem(1L, cart, CartItemType.MERCHANDISE, m1, 3);
        CartItem rewardItem = merchItem(2L, cart, CartItemType.REWARD, r1, 2);

        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(merchItem, rewardItem));

        Invoice invoice = new Invoice();
        invoice.setId(5000L);

        when(invoiceService.purchaseMerchandiseWithRewards(eq(user), anyList(), anyList(), eq(PaymentMethod.CREDIT_CARD), any(PaymentDetailDto.class)))
            .thenReturn(invoice);

        CartCheckoutResultDto result = cartService.checkout(user.getEmail(), PaymentMethod.CREDIT_CARD, validCard());

        assertAll(
            () -> assertThat(result.merchandiseInvoiceId()).isEqualTo(5000L),
            () -> assertThat(result.ticketInvoiceId()).isNull()
        );

        ArgumentCaptor<List<MerchandisePurchaseItemDto>> merchCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<MerchandisePurchaseItemDto>> rewardCaptor = ArgumentCaptor.forClass(List.class);

        verify(invoiceService).purchaseMerchandiseWithRewards(eq(user), merchCaptor.capture(), rewardCaptor.capture(), eq(PaymentMethod.CREDIT_CARD), any(PaymentDetailDto.class));

        assertAll(
            () -> assertThat(merchCaptor.getValue()).hasSize(1),
            () -> assertThat(merchCaptor.getValue().get(0).merchandiseId()).isEqualTo(1L),
            () -> assertThat(merchCaptor.getValue().get(0).quantity()).isEqualTo(3),

            () -> assertThat(rewardCaptor.getValue()).hasSize(1),
            () -> assertThat(rewardCaptor.getValue().get(0).merchandiseId()).isEqualTo(2L),
            () -> assertThat(rewardCaptor.getValue().get(0).quantity()).isEqualTo(2)
        );

        verify(cartItemRepository).deleteAll(List.of(merchItem, rewardItem));
    }

    @Test
    void checkout_ticketOnly_purchasesTickets_andDeletesAll() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Event event = new Event();
        event.setTitle("Test Event");

        Ticket t1 = new Ticket(new Location(), new Seat(), event);
        t1.setId(9001L);
        t1.setNetPrice(50.0);
        t1.setGrossPrice(55.0);

        Ticket t2 = new Ticket(new Location(), new Seat(), event);
        t2.setId(9002L);
        t2.setNetPrice(100.0);
        t2.setGrossPrice(110.0);

        CartItem item1 = ticketItem(1L, cart, t1);
        CartItem item2 = ticketItem(2L, cart, t2);

        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(item1, item2));

        List<DetailedTicketDto> purchasedTickets = List.of(
            new DetailedTicketDto(9001L, "Test Event", 1, 1, 101L, LocalDate.now(), "18:00",
                "INV-1234", "RES-111", 55.0, "Vienna", false, 501L, 11L, 201L, 1234L),
            new DetailedTicketDto(9002L, "Test Event", 1, 2, 102L, LocalDate.now(), "18:00",
                "INV-1234", "RES-111", 110.0, "Vienna", false, 501L, 11L, 201L, 1234L)
        );

        when(ticketService.purchase(List.of(9001L, 9002L), user.getEmail())).thenReturn(purchasedTickets);

        Invoice ticketInvoice = new Invoice();
        ticketInvoice.setId(1234L);
        when(invoiceService.findById(1234L, user.getEmail())).thenReturn(ticketInvoice);

        SimpleInvoiceDto simple = new SimpleInvoiceDto(1234L, "INV-1234", user.getId());

        Invoice invoice = new Invoice();
        invoice.setId(1234L);
        when(invoiceService.findById(1234L, user.getEmail())).thenReturn(invoice);

        CartCheckoutResultDto result = cartService.checkout(user.getEmail(), PaymentMethod.CREDIT_CARD, validCard());

        assertAll(
            () -> assertThat(result.merchandiseInvoiceId()).isNull(),
            () -> assertThat(result.ticketInvoiceId()).isEqualTo(1234L)
        );

        verify(cartItemRepository).deleteAll(List.of(item1, item2));
        verify(invoiceService, never()).purchaseMerchandiseWithRewards(any(), anyList(), anyList(), any(), any());
    }

    @Test
    void checkout_merchRewardAndTicket_executesBothFlows_andDeletesAll() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Merchandise m1 = merch(1L, "M1", "10.00", 50);
        Merchandise r1 = redeemableMerch(2L, "R1", 100, 50);

        CartItem merchItem = merchItem(1L, cart, CartItemType.MERCHANDISE, m1, 1);
        CartItem rewardItem = merchItem(2L, cart, CartItemType.REWARD, r1, 2);

        Event event = new Event();
        event.setId(501L);
        event.setTitle("Concert");

        Ticket t = new Ticket(new Location(), new Seat(), event);
        t.setId(9001L);
        t.setGrossPrice(55.0);

        CartItem ticketItem = ticketItem(3L, cart, t);

        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(merchItem, rewardItem, ticketItem));

        Invoice merchInvoice = new Invoice();
        merchInvoice.setId(8000L);

        when(invoiceService.purchaseMerchandiseWithRewards(eq(user), anyList(), anyList(), eq(PaymentMethod.CREDIT_CARD), any(PaymentDetailDto.class)))
            .thenReturn(merchInvoice);

        List<DetailedTicketDto> purchasedTickets = List.of(
            new DetailedTicketDto(9001L, "Concert", 1, 1, 101L, LocalDate.now(), "18:00",
                "INV-1", "RES-1", 55.0, "Vienna", false, 501L, 11L, 201L, 9000L)
        );
        when(ticketService.purchase(List.of(9001L), user.getEmail())).thenReturn(purchasedTickets);

        Invoice ticketInvoice = new Invoice();
        ticketInvoice.setId(9000L);
        when(invoiceService.findById(9000L, user.getEmail())).thenReturn(ticketInvoice);

        CartCheckoutResultDto result = cartService.checkout(user.getEmail(), PaymentMethod.CREDIT_CARD, validCard());

        assertAll(
            () -> assertThat(result.merchandiseInvoiceId()).isEqualTo(8000L),
            () -> assertThat(result.ticketInvoiceId()).isEqualTo(9000L)
        );

        verify(cartItemRepository).deleteAll(List.of(merchItem, rewardItem, ticketItem));
    }

    // -------------------------
    // addTicket
    // -------------------------

    @Test
    void addTicket_ticketHasInvoice_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Ticket t = new Ticket();
        t.setId(123L);
        t.setInvoice(new Invoice());

        when(ticketRepository.findById(123L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> cartService.addTicket(user.getEmail(), 123L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("already purchased");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addTicket_reservationNull_currentlyThrowsNpe_orShouldThrowValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Ticket t = new Ticket();
        t.setId(123L);
        t.setInvoice(null);
        t.setReservation(null);

        when(ticketRepository.findById(123L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> cartService.addTicket(user.getEmail(), 123L))
            .isInstanceOf(RuntimeException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addTicket_ownerNull_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Reservation r = new Reservation();
        r.setId(1L);
        r.setUser(null);

        Ticket t = new Ticket();
        t.setId(123L);
        t.setInvoice(null);
        t.setReservation(r);

        when(ticketRepository.findById(123L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> cartService.addTicket(user.getEmail(), 123L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("does not belong");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addTicket_ownerIdMismatch_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        User other = new User();
        other.setId(999L);

        Reservation r = new Reservation();
        r.setUser(other);

        Ticket t = new Ticket();
        t.setId(123L);
        t.setInvoice(null);
        t.setReservation(r);

        when(ticketRepository.findById(123L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> cartService.addTicket(user.getEmail(), 123L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("does not belong");

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addTicket_existingCartItem_doesNotCreateNewStillSavesAndReturnsDto() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

        Reservation r = new Reservation();
        r.setUser(user);

        Event e = new Event();
        e.setId(501L);
        e.setTitle("Concert");

        Ticket t = new Ticket();
        t.setId(123L);
        t.setInvoice(null);
        t.setReservation(r);
        t.setEvent(e);
        t.setGrossPrice(55.0);
        Seat seat = new Seat();
        seat.setRowNumber(1);
        seat.setSeatNumber(2);
        t.setSeat(seat);

        when(ticketRepository.findById(123L)).thenReturn(Optional.of(t));

        CartItem existing = new CartItem(CartItemType.TICKET);
        existing.setId(42L);
        existing.setCart(cart);
        existing.setTicket(t);

        when(cartItemRepository.findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, 123L))
            .thenReturn(Optional.of(existing));

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(existing));

        CartDto dto = cartService.addTicket(user.getEmail(), 123L);

        assertAll(
            () -> assertThat(dto.items()).hasSize(1),
            () -> assertThat(dto.items().get(0).type()).isEqualTo(CartItemType.TICKET),
            () -> assertThat(dto.items().get(0).ticketId()).isEqualTo(123L),
            () -> assertThat(dto.items().get(0).eventTitle()).isEqualTo("Concert"),
            () -> assertThat(dto.total()).isEqualByComparingTo(new BigDecimal("55.0"))
        );

        verify(cartItemRepository).save(existing);
    }

    // -------------------------
    // removeTicket
    // -------------------------

    @Test
    void removeTicket_cartMissing_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeTicket(user.getEmail(), 1L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Cart not found");
    }

    @Test
    void removeTicket_cartItemNotFound_throwsNotFound() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeTicket(user.getEmail(), 1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("CartItem not found");
    }

    @Test
    void removeTicket_wrongOwner_throwsValidation() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Cart otherCart = new Cart(user);
        otherCart.setId(777L);

        Ticket t = new Ticket();
        t.setId(123L);

        CartItem item = new CartItem(CartItemType.TICKET);
        item.setId(1L);
        item.setCart(otherCart);
        item.setTicket(t);

        when(cartItemRepository.findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, 123L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeTicket(user.getEmail(), 123L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("does not belong");

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void removeTicket_reservationNull_skipsCountAndReservationDelete() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        Ticket ticket = new Ticket();
        ticket.setId(123L);
        ticket.setReservation(null);

        CartItem item = new CartItem(CartItemType.TICKET);
        item.setId(1L);
        item.setCart(cart);
        item.setTicket(ticket);

        when(cartItemRepository.findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, 123L))
            .thenReturn(Optional.of(item));

        cartService.removeTicket(user.getEmail(), 123L);

        verify(cartItemRepository).delete(item);
        verify(ticketRepository).delete(ticket);
        verify(ticketRepository, never()).countByReservation(any(Reservation.class));
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    // -------------------------
    // addTickets<
    // -------------------------

    @Test
    void addTickets_callsAddTicketForEachId() {
        CartServiceImpl spy = spy(cartService);

        doReturn(new CartDto(1L, List.of(), BigDecimal.ZERO))
            .when(spy).addTicket(eq(user.getEmail()), anyLong());

        List<CartDto> res = spy.addTickets(user.getEmail(), List.of(1L, 2L, 3L));

        assertThat(res).hasSize(3);
        verify(spy, times(3)).addTicket(eq(user.getEmail()), anyLong());
    }

}
