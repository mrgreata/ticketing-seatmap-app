package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.MerchandisePurchaseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.InvoiceMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private MerchandiseRepository merchandiseRepository;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private TicketService ticketService;
    @Mock private PdfService pdfService;


    @InjectMocks private InvoiceServiceImpl invoiceService;

    private User regularUser;
    private User nonRegularUser;

    private Merchandise merchA;
    private Merchandise merchB;
    private Merchandise rewardMerch;

    private User user;
    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    void setup() {
        regularUser = new User();
        regularUser.setId(10L);
        regularUser.setTotalCentsSpent(5000L);
        regularUser.setRewardPoints(0);

        nonRegularUser = new User();
        nonRegularUser.setId(11L);
        nonRegularUser.setTotalCentsSpent(0L);
        nonRegularUser.setRewardPoints(0);

        merchA = new Merchandise();
        merchA.setId(1L);
        merchA.setName("A");
        merchA.setUnitPrice(new BigDecimal("10.00"));
        merchA.setRemainingQuantity(10);
        merchA.setRewardPointsPerUnit(7);
        merchA.setRedeemableWithPoints(false);
        merchA.setPointsPrice(null);

        merchB = new Merchandise();
        merchB.setId(2L);
        merchB.setName("B");
        merchB.setUnitPrice(new BigDecimal("2.50"));
        merchB.setRemainingQuantity(10);
        merchB.setRewardPointsPerUnit(3);
        merchB.setRedeemableWithPoints(false);
        merchB.setPointsPrice(null);

        rewardMerch = new Merchandise();
        rewardMerch.setId(5L);
        rewardMerch.setName("Reward");
        rewardMerch.setUnitPrice(new BigDecimal("0.00"));
        rewardMerch.setRemainingQuantity(10);
        rewardMerch.setRewardPointsPerUnit(0);
        rewardMerch.setRedeemableWithPoints(true);
        rewardMerch.setPointsPrice(100);

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.at");
        user.setFirstName("FirstTest");
        user.setLastName("LastTest");
        user.setAddress("Address");

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Test Event");

        Location location = new Location();
        location.setId(20L);

        Seat seat = new Seat();
        seat.setId(30L);

        ticket1 = new Ticket(location, seat, event);
        ticket1.setId(100L);
        ticket1.setNetPrice(50.0);
        ticket1.setTaxRate(10.0);

        ticket2 = new Ticket(location, seat, event);
        ticket2.setId(101L);
        ticket2.setNetPrice(100.0);
        ticket2.setTaxRate(20.0);
    }

    private void stubInvoiceSavesPassThrough() {
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUserSavePassThrough() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaymentDetailDto validCard() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        String mmyy = String.format("%02d%02d", nextMonth.getMonthValue(), nextMonth.getYear() % 100);
        return new PaymentDetailDto("4242424242424242", mmyy, "123", null);
    }

    private PaymentDetailDto invalidCardNumber() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        String mmyy = String.format("%02d%02d", nextMonth.getMonthValue(), nextMonth.getYear() % 100);
        return new PaymentDetailDto("123", mmyy, "123", null);
    }

    private PaymentDetailDto invalidLuhnCardNumber() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        String mmyy = String.format("%02d%02d", nextMonth.getMonthValue(), nextMonth.getYear() % 100);
        return new PaymentDetailDto("4242424242424243", mmyy, "123", null);
    }

    private PaymentDetailDto invalidExpiryFormat() {
        return new PaymentDetailDto("4242424242424242", "1325", "123", null);
    }

    private PaymentDetailDto invalidCvc() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        String mmyy = String.format("%02d%02d", nextMonth.getMonthValue(), nextMonth.getYear() % 100);
        return new PaymentDetailDto("4242424242424242", mmyy, "12", null);
    }

    private PaymentDetailDto expiredCard() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String mmyy = String.format("%02d%02d", lastMonth.getMonthValue(), lastMonth.getYear() % 100);
        return new PaymentDetailDto("4242424242424242", mmyy, "123", null);
    }

    // -------------------------------------------------------------------------
    // purchaseMerchandiseWithRewards
    // -------------------------------------------------------------------------

    @Test
    void purchaseMerchandiseWithRewards_success_regularUser_merchMerged_pointsAdded_totalCentsUpdated() {
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(merchA));
        when(merchandiseRepository.findById(2L)).thenReturn(Optional.of(merchB));
        stubInvoiceSavesPassThrough();
        stubUserSavePassThrough();

        List<MerchandisePurchaseItemDto> merchItems = List.of(
            new MerchandisePurchaseItemDto(1L, 2),
            new MerchandisePurchaseItemDto(1L, 1),
            new MerchandisePurchaseItemDto(2L, 4)
        );

        Invoice invoice = invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            merchItems,
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        );

        assertAll(
            () -> assertThat(invoice).isNotNull(),
            () -> assertThat(regularUser.getTotalCentsSpent()).isEqualTo(9000L),
            () -> assertThat(regularUser.getRewardPoints()).isEqualTo(33),
            () -> assertThat(invoice.getMerchandiseItems()).hasSize(2),
            () -> assertThat(invoice.getMerchandiseItems())
                .anyMatch(li -> li.getMerchandise().getId().equals(1L) && li.getQuantity() == 3 && !Boolean.TRUE.equals(li.getRedeemedWithPoints()))
                .anyMatch(li -> li.getMerchandise().getId().equals(2L) && li.getQuantity() == 4 && !Boolean.TRUE.equals(li.getRedeemedWithPoints()))
        );

        verify(userRepository).save(regularUser);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void purchaseMerchandiseWithRewards_success_nonRegularUser_stillNotRegular_noPointsAdded() {
        when(merchandiseRepository.findById(2L)).thenReturn(Optional.of(merchB));
        stubInvoiceSavesPassThrough();
        stubUserSavePassThrough();

        List<MerchandisePurchaseItemDto> merchItems = List.of(
            new MerchandisePurchaseItemDto(2L, 1)
        );

        Invoice invoice = invoiceService.purchaseMerchandiseWithRewards(
            nonRegularUser,
            merchItems,
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        );

        assertAll(
            () -> assertThat(invoice).isNotNull(),
            () -> assertThat(nonRegularUser.getTotalCentsSpent()).isEqualTo(250L),
            () -> assertThat(nonRegularUser.getRewardPoints()).isEqualTo(0)
        );

        verify(userRepository).save(nonRegularUser);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void purchaseMerchandiseWithRewards_userNull_throwsNotFound() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            null,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found");

        verify(invoiceRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_noItems_throwsValidation() {
        assertAll(
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser, null, null, PaymentMethod.CREDIT_CARD, validCard()
            )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("At least one item"),
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser, List.of(), List.of(), PaymentMethod.CREDIT_CARD, validCard()
            )).isInstanceOf(ValidationException.class)
                .hasMessageContaining("At least one item")
        );

        verify(invoiceRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_merchItemsNullOrEmpty_throwsValidation() {
        assertAll(
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser, null, List.of(), PaymentMethod.CREDIT_CARD, validCard()
            )).isInstanceOf(ValidationException.class),
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser, List.of(), List.of(), PaymentMethod.CREDIT_CARD, validCard()
            )).isInstanceOf(ValidationException.class)
        );

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_itemNullInsideList_throwsValidation() {
        List<MerchandisePurchaseItemDto> items = new ArrayList<>();
        items.add(new MerchandisePurchaseItemDto(1L, 1));
        items.add(null);

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            items,
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("is null");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_itemsWithNullMerchandiseId_throwsValidation() {
        MerchandisePurchaseItemDto bad = new MerchandisePurchaseItemDto(null, 1);

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(bad),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("has no merchandise id");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_quantityNullOrZero_throwsValidation() {
        MerchandisePurchaseItemDto qNull = new MerchandisePurchaseItemDto(1L, null);
        MerchandisePurchaseItemDto qZero = new MerchandisePurchaseItemDto(1L, 0);

        assertAll(
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser,
                List.of(qNull),
                List.of(),
                PaymentMethod.CREDIT_CARD,
                validCard()
            )).isInstanceOf(ValidationException.class),
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser,
                List.of(qZero),
                List.of(),
                PaymentMethod.CREDIT_CARD,
                validCard()
            )).isInstanceOf(ValidationException.class)
        );

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_paymentMethodNull_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            null,
            validCard()
        )).isInstanceOf(ValidationException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_paymentDetailNull_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            null
        )).isInstanceOf(ValidationException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_creditCard_invalidNumber_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            invalidCardNumber()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_creditCard_invalidLuhnNumber_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            invalidLuhnCardNumber()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid card number");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_creditCard_invalidExpiryFormat_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            invalidExpiryFormat()
        )).isInstanceOf(ValidationException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_creditCard_invalidCVC_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            invalidCvc()
        )).isInstanceOf(ValidationException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_creditCard_expired_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            expiredCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("expired");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_merchNotFound_throwsNotFound() {
        when(merchandiseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(99L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_unitPriceMoreThanTwoDecimals_throwsValidation() {
        merchA.setUnitPrice(new BigDecimal("10.999"));
        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(merchA));

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid unitPrice for merchandiseId=1");

        verify(invoiceRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_success_merchAndRewards_createsLines_marksRedeemed() {
        User u = new User();
        u.setId(20L);
        u.setRewardPoints(1000);
        u.setTotalCentsSpent(5000L);

        when(merchandiseRepository.findById(1L)).thenReturn(Optional.of(merchA));
        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(rewardMerch));
        stubInvoiceSavesPassThrough();
        stubUserSavePassThrough();

        Invoice invoice = invoiceService.purchaseMerchandiseWithRewards(
            u,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(new MerchandisePurchaseItemDto(5L, 3)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        );

        assertAll(
            () -> assertThat(invoice).isNotNull(),
            () -> assertThat(invoice.getMerchandiseItems()).hasSize(2),
            () -> assertThat(invoice.getMerchandiseItems())
                .anyMatch(li -> li.getMerchandise().getId().equals(1L) && li.getQuantity() == 1 && !Boolean.TRUE.equals(li.getRedeemedWithPoints()))
                .anyMatch(li -> li.getMerchandise().getId().equals(5L) && li.getQuantity() == 3 && Boolean.TRUE.equals(li.getRedeemedWithPoints()))
        );
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(userRepository).save(u);
    }

    @Test
    void purchaseMerchandiseWithRewards_rewards_notRegularCustomer_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            nonRegularUser,
            List.of(new MerchandisePurchaseItemDto(1L, 1)),
            List.of(new MerchandisePurchaseItemDto(5L, 1)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("not a regular customer");

        verify(invoiceRepository, never()).save(any());
        verify(merchandiseRepository, never()).findById(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_rewardMerchNotFound_throwsNotFound() {
        when(merchandiseRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(),
            List.of(new MerchandisePurchaseItemDto(123L, 1)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Merchandise not found");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_rewardNotRedeemable_throwsValidation() {
        Merchandise notRedeemable = new Merchandise();
        notRedeemable.setId(9L);
        notRedeemable.setRedeemableWithPoints(false);
        notRedeemable.setPointsPrice(100);

        when(merchandiseRepository.findById(9L)).thenReturn(Optional.of(notRedeemable));

        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(),
            List.of(new MerchandisePurchaseItemDto(9L, 1)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("not redeemable");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_rewardPointsPriceNullOrNonPositive_throwsValidation() {
        Merchandise mNull = new Merchandise();
        mNull.setId(7L);
        mNull.setRedeemableWithPoints(true);
        mNull.setPointsPrice(null);

        Merchandise mZero = new Merchandise();
        mZero.setId(8L);
        mZero.setRedeemableWithPoints(true);
        mZero.setPointsPrice(0);

        when(merchandiseRepository.findById(7L)).thenReturn(Optional.of(mNull));
        when(merchandiseRepository.findById(8L)).thenReturn(Optional.of(mZero));

        assertAll(
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser,
                List.of(),
                List.of(new MerchandisePurchaseItemDto(7L, 1)),
                PaymentMethod.CREDIT_CARD,
                validCard()
            )).isInstanceOf(ValidationException.class),
            () -> assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
                regularUser,
                List.of(),
                List.of(new MerchandisePurchaseItemDto(8L, 1)),
                PaymentMethod.CREDIT_CARD,
                validCard()
            )).isInstanceOf(ValidationException.class)
        );

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void purchaseMerchandiseWithRewards_rewardQuantityLessThanOne_throwsValidation() {
        assertThatThrownBy(() -> invoiceService.purchaseMerchandiseWithRewards(
            regularUser,
            List.of(),
            List.of(new MerchandisePurchaseItemDto(5L, 0)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void purchaseMerchandiseWithRewards_rewardOnly_regularUser_succeeds() {
        User u = new User();
        u.setId(30L);
        u.setTotalCentsSpent(5000L);
        u.setRewardPoints(1000);

        when(merchandiseRepository.findById(5L)).thenReturn(Optional.of(rewardMerch));
        stubInvoiceSavesPassThrough();

        Invoice invoice = invoiceService.purchaseMerchandiseWithRewards(
            u,
            List.of(),
            List.of(new MerchandisePurchaseItemDto(5L, 1)),
            PaymentMethod.CREDIT_CARD,
            validCard()
        );

        assertAll(
            () -> assertThat(invoice).isNotNull(),
            () -> assertThat(invoice.getMerchandiseItems()).hasSize(1),
            () -> assertThat(invoice.getMerchandiseItems())
                .anyMatch(li ->
                    li.getMerchandise().getId().equals(5L)
                        && li.getQuantity() == 1
                        && Boolean.TRUE.equals(li.getRedeemedWithPoints())
                )
        );

        verify(invoiceRepository, times(1)).save(any(Invoice.class));

        verify(userRepository, never()).save(any(User.class));
    }

    // -------------------------------------------------------------------------
    // create invoice with tickets (unchanged API)
    // -------------------------------------------------------------------------

    @Test
    void createInvoiceWithTickets_success() {
        InvoiceCreateDto dto = new InvoiceCreateDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            LocalDateTime.now(),
            List.of(ticket1.getId(), ticket2.getId())
        );

        when(userService.findById(user.getId())).thenReturn(user);
        when(ticketService.findAllByIds(dto.ticketIds())).thenReturn(List.of(ticket1, ticket2));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invObj = inv.getArgument(0);
            if (invObj.getId() == null) {
                invObj.setId(1L);
            }
            return invObj;
        });

        when(invoiceMapper.toSimple(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invObj = inv.getArgument(0);
            return new SimpleInvoiceDto(invObj.getId(), invObj.getInvoiceNumber(), invObj.getUser().getId());
        });

        SimpleInvoiceDto result = invoiceService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(user.getId());

        assertThat(ticket1.getInvoice()).isNotNull();
        assertThat(ticket2.getInvoice()).isNotNull();
        assertThat(ticket1.getInvoice().getTickets()).contains(ticket1, ticket2);
        assertThat(ticket2.getInvoice().getTickets()).contains(ticket1, ticket2);

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(ticketService).findAllByIds(dto.ticketIds());
    }

    @Test
    void createInvoiceWithTickets_userNotFound_throwsNotFound() {
        InvoiceCreateDto dto = new InvoiceCreateDto(99L, "1", "2", "3", LocalDateTime.now(), List.of(100L, 101L));
        when(userService.findById(99L)).thenThrow(new NotFoundException("User not found"));

        assertThatThrownBy(() -> invoiceService.create(dto))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createInvoiceWithTickets_ticketNotFound_throwsNotFound() {
        InvoiceCreateDto dto = new InvoiceCreateDto(user.getId(), "1", "2", "3", LocalDateTime.now(), List.of(100L, 101L));
        when(userService.findById(user.getId())).thenReturn(user);
        when(ticketService.findAllByIds(dto.ticketIds())).thenReturn(List.of()); // keine Tickets gefunden

        assertThatThrownBy(() -> invoiceService.create(dto))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("No tickets found for ids");
    }

    @Test
    void getMyCancelledTickets_returnsList() {
        CancelledTicketDto dto = new CancelledTicketDto(
            1L,
            "Concert X",
            LocalDateTime.of(2026, 1, 25, 18, 0),
            LocalDate.of(2026, 1, 24),
            List.of("A1", "A2"),
            42L,
            true
        );
        when(ticketService.getMyCancelledTickets("user@test.com"))
            .thenReturn(List.of(dto));

        List<CancelledTicketDto> result = ticketService.getMyCancelledTickets("user@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).eventName()).isEqualTo("Concert X");
    }

    @Test
    void createCreditInvoice_success() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        Invoice originalInvoice = new Invoice();
        originalInvoice.setInvoiceNumber("INV-123");
        ticket.setInvoice(originalInvoice);
        Event event = new Event();
        event.setDateTime(LocalDateTime.now());
        ticket.setEvent(event);

        when(ticketService.findById(1L)).thenReturn(ticket);
        when(ticketService.createCancelledTickets(
            eq(List.of(1L)),
            eq("user@test.com"),
            any(Invoice.class)
        )).thenReturn(List.of(new CancelledTicket()));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        User user = new User();
        user.setId(1L);
        when(userService.findByEmail("user@test.com")).thenReturn(user);

        Invoice creditInvoice = invoiceService.createCreditInvoice(List.of(1L), "user@test.com");

        assertThat(creditInvoice).isNotNull();
        assertThat(creditInvoice.getUser()).isEqualTo(user);
        verify(ticketService).deleteByIds(List.of(1L), "user@test.com");
    }

    @Test
    void createCreditInvoice_ticketNotFound_throws() {
        when(ticketService.findById(1L)).thenThrow(new NotFoundException("Ticket not found"));
        when(userService.findByEmail("user@test.com")).thenReturn(user);

        assertThatThrownBy(() -> invoiceService.createCreditInvoice(List.of(1L), "user@test.com"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Ticket not found");
    }
    @Test
    void downloadInvoicePdf_success() {
        Invoice inv = new Invoice();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(pdfService.generateInvoicePdf(inv)).thenReturn(new byte[]{1,2,3});

        byte[] pdf = invoiceService.downloadInvoicePdf(1L);
        assertThat(pdf).containsExactly(1,2,3);
    }

    @Test
    void downloadInvoicePdf_invoiceNotFound_throws() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.downloadInvoicePdf(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Invoice not found");
    }

    @Test
    void findById_success() {
        Invoice inv = new Invoice();
        User user = new User();
        user.setEmail("a@b.com");
        inv.setUser(user);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThat(invoiceService.findById(1L, "a@b.com")).isEqualTo(inv);
    }

    @Test
    void findById_notFound_throws() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.findById(1L, "a@b.com"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getInvoiceEntityForUser_wrongUser_throwsAccessDenied() {
        User invoiceOwner = new User();
        invoiceOwner.setEmail("owner@test.com");
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setUser(invoiceOwner);

        InvoiceServiceImpl spyService = Mockito.spy(invoiceService);
        Mockito.doReturn(invoice).when(spyService).findInvoiceForUserOrThrow(1L, "hacker@test.com");

        assertThatThrownBy(() -> spyService.getInvoiceEntityForUser(1L, "hacker@test.com"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied for invoice");
    }


}
