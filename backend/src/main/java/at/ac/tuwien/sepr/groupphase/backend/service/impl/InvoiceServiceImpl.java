package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.CreditInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.MerchandisePurchaseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.InvoiceMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceMerchandiseItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.CancelledTicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfService;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long REGULAR_USER_THRESHOLD_CENTS = 5000;

    private final InvoiceRepository invoiceRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final InvoiceMapper invoiceMapper;
    private final PdfService pdfService;

    @Lazy
    private final TicketService ticketService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, MerchandiseRepository merchandiseRepository,
                              UserService userService, UserRepository userRepository,
                              InvoiceMapper invoiceMapper, PdfService pdfService, @Lazy TicketService ticketService,
                              CancelledTicketRepository cancelledTicketRepository) {
        this.invoiceRepository = invoiceRepository;
        this.merchandiseRepository = merchandiseRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.invoiceMapper = invoiceMapper;
        this.pdfService = pdfService;
        this.ticketService = ticketService;
    }

    private User findUserOrThrow(String email) {
        return userService.findByEmail(email);
    }

    private User findUserOrThrow(Long userId) {
        return userService.findById(userId);
    }


    @Transactional
    protected Invoice findInvoiceForUserOrThrow(Long invoiceId, String userEmail) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

        if (!invoice.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied for invoice: " + invoiceId);
        }

        return invoice;
    }

    private void verifyInvoiceOwnership(Invoice invoice, String userEmail) throws AccessDeniedException {
        if (!invoice.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied for invoice: " + invoice.getId());
        }
    }

    @Transactional
    @Override
    public byte[] downloadInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));
        return pdfService.generateInvoicePdf(invoice);
    }


    @Override
    public List<Invoice> findAll() {
        LOGGER.trace("Find all invoices");
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Invoice findById(Long id, String userEmail) {
        return findInvoiceForUserOrThrow(id, userEmail);
    }


    @Transactional(readOnly = true)
    @Override
    public Invoice findCreditInvoiceForUserWithTickets(Long id, String email) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Credit invoice not found: " + id));

        if (!invoice.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Access denied for invoice: " + id);
        }

        invoice.getCancelledTickets().size(); // triggers lazy loading

        return invoice;
    }


    @Transactional
    public Invoice createCreditInvoice(List<Long> ticketIdsToCancel, String userEmail) throws AccessDeniedException {

        User user = findUserOrThrow(userEmail);
        if (ticketIdsToCancel == null || ticketIdsToCancel.isEmpty()) {
            throw new ValidationException("Ticket list must not be empty");
        }

        //can do that because method only called with tickets with same invoices
        Ticket ticket = ticketService.findById(ticketIdsToCancel.get(0));
        Invoice originalInvoice = ticket.getInvoice();

        if (originalInvoice == null) {
            throw new ValidationException("Cannot create a credit invoice for a ticket that has no original invoice");
        }

        Invoice creditInvoice = new Invoice();
        creditInvoice.setUser(user);
        creditInvoice.setOriginalInvoiceNumber(originalInvoice.getInvoiceNumber());
        creditInvoice.setInvoiceCancellationDate(LocalDate.now());
        creditInvoice.setInvoiceDate(originalInvoice.getInvoiceDate());
        creditInvoice.setInvoiceNumber(generateInvoiceNumber());
        creditInvoice.setEventDate(ticket.getEvent().getDateTime());

        Invoice savedCreditInvoice = invoiceRepository.save(creditInvoice);
        creditInvoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + creditInvoice.getId());
        invoiceRepository.save(creditInvoice);


        List<CancelledTicket> cancelledTickets =
            ticketService.createCancelledTickets(ticketIdsToCancel, userEmail, savedCreditInvoice);

        cancelledTickets.forEach(savedCreditInvoice::addCancelledTicket);

        ticketService.deleteByIds(ticketIdsToCancel, userEmail);
        return savedCreditInvoice;
    }


    @Override
    public SimpleInvoiceDto create(InvoiceCreateDto dto) {
        User user = findUserOrThrow(dto.userId());

        List<Ticket> tickets = ticketService.findAllByIds(dto.ticketIds());
        checkTicketsNotEmpty(tickets, dto.ticketIds());

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setTickets(tickets);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setEventDate(dto.eventDate());
        invoice.setInvoiceNumber(
            "INV-" + System.currentTimeMillis()
        );
        double netTotal = tickets.stream().mapToDouble(Ticket::getNetPrice).sum();

        invoice.setNetTotal(netTotal);

        double taxTotal = tickets.stream().mapToDouble(t -> t.getNetPrice() * t.getTaxRate() / 100).sum();

        invoice.setTaxTotal(taxTotal);

        double grossTotal = netTotal + taxTotal;

        invoice.setGrossTotal(grossTotal);

        tickets.forEach(t -> t.setInvoice(invoice));
        invoice.setTickets(tickets);
        Invoice saved = invoiceRepository.save(invoice);

        return invoiceMapper.toSimple(saved);
    }

    private void checkTicketsNotEmpty(List<Ticket> tickets, List<Long> ticketIds) {
        if (tickets == null || tickets.isEmpty()) {
            throw new NotFoundException("No tickets found for ids: " + ticketIds);
        }
    }

    @Override
    public Invoice purchaseMerchandiseWithRewards(User user, List<MerchandisePurchaseItemDto> merchItems, List<MerchandisePurchaseItemDto> rewardItems,
                                                  PaymentMethod paymentMethod, PaymentDetailDto paymentDetail) {
        if (user == null) {
            throw new NotFoundException("User not found!");
        }
        boolean hasMerch = merchItems != null && !merchItems.isEmpty();
        boolean hasRewards = rewardItems != null && !rewardItems.isEmpty();

        if (!hasMerch && !hasRewards) {
            throw new ValidationException("At least one item must be purchased/redeemed");
        }

        if (hasRewards && !isRegularCustomer(user)) {
            throw new ValidationException("User is not a regular customer, cannot redeem reward");
        }

        if (hasMerch) {
            validatePurchase(merchItems, paymentMethod, paymentDetail);
        }

        Map<Long, Integer> mergedMerch = hasMerch ? mergeItems(merchItems) : Map.of();
        Map<Long, Integer> mergedRewards = hasRewards ? mergeItems(rewardItems) : Map.of();

        long purchaseTotalCents = 0L;
        int pointsEarnedThisPurchase = 0;

        Map<Long, Merchandise> merchProducts = new LinkedHashMap<>();
        Map<Long, Merchandise> rewardProducts = new LinkedHashMap<>();

        for (var entry : mergedMerch.entrySet()) {
            Long id = entry.getKey();
            int qty = entry.getValue();

            if (qty < 1) {
                throw new ValidationException("Quantity for merchandise " + id + " must be positive");
            }

            Merchandise product = merchandiseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchandise not found: " + id));

            long unitCents;
            try {
                unitCents = product.getUnitPrice().movePointRight(2).longValueExact();
            } catch (ArithmeticException e) {
                throw new ValidationException(
                    "Invalid unitPrice for merchandiseId=" + id + " (must have max 2 decimal places)"
                );
            }

            purchaseTotalCents += unitCents * (long) qty;
            pointsEarnedThisPurchase += product.getRewardPointsPerUnit() * qty;

            merchProducts.put(id, product);
        }

        for (var entry : mergedRewards.entrySet()) {
            Long id = entry.getKey();
            int qty = entry.getValue();

            if (qty < 1) {
                throw new ValidationException("Quantity for merchandise " + id + " must be positive");
            }

            Merchandise product = merchandiseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchandise not found: " + id));

            if (!Boolean.TRUE.equals(product.getRedeemableWithPoints())) {
                throw new ValidationException("Merchandise " + id + " is not redeemable with points");
            }

            int pointsPrice = safePoints(product.getPointsPrice());
            if (pointsPrice <= 0) {
                throw new ValidationException("Points price for merchandise " + id + " must be positive");
            }

            rewardProducts.put(id, product);
        }

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setEventDate(LocalDateTime.now());

        for (var entry : mergedMerch.entrySet()) {
            Long id = entry.getKey();
            int qty = entry.getValue();

            Merchandise product = merchProducts.get(id);
            if (product == null) {
                throw new IllegalStateException("Missing merchandise entity for id=" + id);
            }

            InvoiceMerchandiseItem line = new InvoiceMerchandiseItem(invoice, product, qty);
            invoice.addMerchandiseItem(line);
        }

        for (var entry : mergedRewards.entrySet()) {
            Long id = entry.getKey();
            int qty = entry.getValue();

            Merchandise product = rewardProducts.get(id);
            if (product == null) {
                throw new IllegalStateException("Missing reward merchandise entity for id=" + id);
            }

            InvoiceMerchandiseItem line = new InvoiceMerchandiseItem(invoice, product, qty);
            line.setRedeemedWithPoints(true);
            invoice.addMerchandiseItem(line);
        }

        invoice = invoiceRepository.save(invoice);

        if (hasMerch) {
            user.setTotalCentsSpent(user.getTotalCentsSpent() + purchaseTotalCents);

            if (isRegularCustomer(user)) {
                user.setRewardPoints(user.getRewardPoints() + pointsEarnedThisPurchase);
            }
            userRepository.save(user);
        }

        return invoice;

    }

    @Override
    public void validatePaymentData(PaymentMethod paymentMethod, PaymentDetailDto paymentDetail) {
        if (paymentMethod == null) {
            throw new ValidationException("Payment method must be provided");
        }
        if (paymentDetail == null) {
            throw new ValidationException("Payment detail must be provided");
        }

        switch (paymentMethod) {
            case CREDIT_CARD -> validateCreditCard(paymentDetail);
            case PAYPAL -> {

            }
            default -> throw new ValidationException("Invalid payment method: " + paymentMethod);
        }
    }


    @Transactional
    @Override
    public List<DetailedInvoiceDto> getMyInvoices(String userEmail) {
        List<Invoice> myInvoices = invoiceRepository.findAllActiveByUserEmailOrderByInvoiceDateDesc(userEmail);
        for (Invoice invoice : myInvoices) {
            findInvoiceForUserOrThrow(invoice.getId(), userEmail);
        }
        return myInvoices
            .stream()
            .map(invoiceMapper::toDetailed)
            .toList();
    }

    @Transactional
    @Override
    public List<CreditInvoiceDto> getMyCreditInvoices(String userEmail) {
        return invoiceRepository.findByUserEmailAndOriginalInvoiceNumberIsNotNull(userEmail)
            .stream()
            .map(invoiceMapper::toCreditInvoiceDto)
            .toList();
    }


    @Transactional(readOnly = true)
    @Override
    public List<SimpleInvoiceDto> getMyMerchandiseInvoices(String userEmail) {
        return invoiceRepository.findAllByUserEmailOrderByInvoiceDateDesc(userEmail).stream()
            .filter(i -> i.getMerchandiseItems() != null && !i.getMerchandiseItems().isEmpty())
            .filter(i -> i.getTickets() == null || i.getTickets().isEmpty())
            .map(invoiceMapper::toSimple)
            .toList();
    }

    @Override
    public DetailedInvoiceDto getInvoiceDetailsForUser(Long invoiceId, String userEmail) {
        Invoice invoice = getInvoiceEntityForUser(invoiceId, userEmail);
        return invoiceMapper.toDetailed(invoice);
    }

    @Override
    public Invoice getInvoiceEntityForUser(Long invoiceId, String userEmail) {
        Invoice invoice = findInvoiceForUserOrThrow(invoiceId, userEmail);
        verifyInvoiceOwnership(invoice, userEmail);
        return invoice;
    }

    private void validatePurchase(
        List<MerchandisePurchaseItemDto> items,
        PaymentMethod paymentMethod,
        PaymentDetailDto paymentDetail
    ) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("At least one item must be purchased");
        }

        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            if (item == null) {
                throw new ValidationException("Item at index " + i + " is null");
            }
            if (item.merchandiseId() == null) {
                throw new ValidationException("Item at index " + i + " has no merchandise id");
            }

            if (item.quantity() == null || item.quantity() < 1) {
                throw new ValidationException("Quantity for item at index " + i + "must be a positive number > 0");
            }
        }
        if (paymentMethod == null) {
            throw new ValidationException("Payment method must be provided");
        }

        if (paymentDetail == null) {
            throw new ValidationException("Payment detail must be provided");
        }
        validatePayment(paymentMethod, paymentDetail);
    }

    private void validatePayment(PaymentMethod method, PaymentDetailDto d) {
        switch (method) {
            case CREDIT_CARD -> validateCreditCard(d);
            case PAYPAL -> {

            }
            default -> throw new ValidationException("Invalid payment method: " + method);
        }
    }

    private static final Pattern CARD_16 = Pattern.compile("^\\d{16}$");
    private static final Pattern MMYY = Pattern.compile("^(0[1-9]|1[0-2])\\d{2}$");
    private static final Pattern CVC = Pattern.compile("^\\d{3}$");

    private void validateCreditCard(PaymentDetailDto d) {


        if (isBlank(d.cardNumber()) || !CARD_16.matcher(d.cardNumber()).matches()) {
            throw new ValidationException("Card number must be 16 digits");
        }

        if (!validateWithLuhnsAlgorithm(d.cardNumber())) {
            throw new ValidationException("Invalid card number");
        }

        if (isBlank(d.expiryMonthYear()) || !MMYY.matcher(d.expiryMonthYear()).matches()) {
            throw new ValidationException("Expiry date must be in MMYY format");
        }

        if (isBlank(d.cvc()) || !CVC.matcher(d.cvc()).matches()) {
            throw new ValidationException("CVC must be 3 digits");
        }

        YearMonth expiry = parseExpiry(d.expiryMonthYear());

        if (expiry.isBefore(YearMonth.now())) {
            throw new ValidationException("Credit card is expired");
        }
    }

    private Map<Long, Integer> mergeItems(List<MerchandisePurchaseItemDto> items) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (MerchandisePurchaseItemDto item : items) {
            result.merge(item.merchandiseId(), item.quantity(), Integer::sum);
        }
        return result;
    }

    private YearMonth parseExpiry(String mmyy) {
        int month = Integer.parseInt(mmyy.substring(0, 2));
        int year = 2000 + Integer.parseInt(mmyy.substring(2));
        return YearMonth.of(year, month);
    }

    private Boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String generateInvoiceNumber() {
        String datePart = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String randomSuffix = Integer.toHexString((int) (Math.random() * 0xFFFFF));

        return "INV-" + datePart + "-" + randomSuffix;
    }

    private int safePoints(Integer p) {
        return p == null ? 0 : Math.max(0, p);
    }

    private boolean isRegularCustomer(User user) {
        return user.getTotalCentsSpent() >= REGULAR_USER_THRESHOLD_CENTS;
    }

    private boolean validateWithLuhnsAlgorithm(String cardNumber) {
        int numberOfDigits = cardNumber.length();
        int sum = 0;
        boolean isSecond = false;

        for (int i = numberOfDigits - 1; i >= 0; i--) {
            int d = cardNumber.charAt(i) - '0';
            if (isSecond) {
                d = d * 2;
            }
            sum += d / 10;
            sum += d % 10;

            isSecond = !isSecond;
        }
        return (sum % 10 == 0);
    }
}
