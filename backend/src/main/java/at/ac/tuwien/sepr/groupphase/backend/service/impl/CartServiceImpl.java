package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutResultDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.MerchandisePurchaseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import at.ac.tuwien.sepr.groupphase.backend.entity.CartItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
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
import at.ac.tuwien.sepr.groupphase.backend.service.CartService;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {


    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final MerchandiseRepository merchandiseRepository;
    private final InvoiceService invoiceService;
    private final TicketService ticketService;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    private final UserRepository userRepository;

    public CartServiceImpl(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        UserService userService,
        MerchandiseRepository merchandiseRepository,
        InvoiceService invoiceService,
        TicketService ticketService,
        ReservationRepository reservationRepository,
        TicketRepository ticketRepository,
        UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userService = userService;
        this.merchandiseRepository = merchandiseRepository;
        this.invoiceService = invoiceService;
        this.ticketService = ticketService;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public CartDto getMyCart(String userEmail) {
        LOGGER.debug("Get cart for userEmail='{}'", userEmail);
        User user = userService.findByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseGet(() -> cartRepository.save(new Cart(user)));
        return toDto(cart);
    }

    @Transactional
    @Override
    public CartDto addMerchandiseItem(String userEmail, Long merchandiseId, int quantity, Boolean redeemedWithPoints) {
        LOGGER.debug("Add merchandise item to cart: userEmail='{}', merchandiseId={}, quantity={}, redeemedWithPoints={}",
            userEmail, merchandiseId, quantity, redeemedWithPoints);
        if (merchandiseId == null) {
            throw new ValidationException("merchandiseId must be provided!");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero!");
        }

        User user = userService.findByEmail(userEmail);

        CartItemType type = Boolean.TRUE.equals(redeemedWithPoints) ? CartItemType.REWARD : CartItemType.MERCHANDISE;

        Merchandise merch = merchandiseRepository.findById(merchandiseId)
            .orElseThrow(() -> new NotFoundException("Merchandise not found: " + merchandiseId));

        if (type == CartItemType.REWARD) {
            if (!isRegularCustomer(user)) {
                throw new ValidationException("User is not a regular customer, cannot redeem rewards");
            }
            requireRedeemable(merch);
            int pp = requirePointsPrice(merch);

            long cost = (long) pp * (long) quantity;
            if (cost > Integer.MAX_VALUE) {
                throw new ValidationException("Points cost too large");
            }
            if ((long) user.getRewardPoints() < cost) {
                throw new ValidationException("Insufficient reward points");
            }

            user.setRewardPoints(user.getRewardPoints() - (int) cost);
            userRepository.save(user);
        }

        if (merch.getRemainingQuantity() < quantity) {
            throw new ValidationException("Quantity for merchandise " + merchandiseId + " exceeds remaining quantity");
        }

        merch.setRemainingQuantity(merch.getRemainingQuantity() - quantity);
        merchandiseRepository.save(merch);

        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseGet(() -> cartRepository.save(new Cart(user)));

        CartItem item = cartItemRepository.findByCartIdAndTypeAndMerchandiseId(cart.getId(), type, merchandiseId)
            .orElseGet(() -> {
                CartItem ci = new CartItem(type);
                ci.setCart(cart);
                ci.setMerchandise(merch);
                ci.setQuantity(0);
                return ci;
            });

        int newQuantity = (item.getQuantity() == null ? 0 : item.getQuantity()) + quantity;
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return toDto(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    @Override
    public CartDto updateMerchandiseItemQuantity(String userEmail, Long cartItemId, int quantity) {
        LOGGER.debug("Update cart item quantity: userEmail='{}', cartItemId={}, newQuantity={}", userEmail, cartItemId, quantity);
        if (cartItemId == null) {
            throw new ValidationException("CartItemId must be provided!");
        }
        if (quantity < 0) {
            throw new ValidationException("Quantity must be greater than zero!");
        }

        User user = userService.findByEmail(userEmail);

        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ValidationException("Cart not found for user!"));

        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new NotFoundException("CartItem not found for cartItemId: " + cartItemId));

        assertOwnership(cart, item);

        if (item.getType() != CartItemType.MERCHANDISE && item.getType() != CartItemType.REWARD) {
            throw new ValidationException("Only merchandise can be updated in this endpoint");
        }

        Merchandise merch = item.getMerchandise();
        if (merch == null) {
            throw new ValidationException("Invalid cart state, merchandise is null for cartItemId: " + cartItemId);
        }

        int oldQty = item.getQuantity() == null ? 0 : item.getQuantity();
        int newQty = quantity;

        if (item.getType() == CartItemType.REWARD) {
            if (!isRegularCustomer(user)) {
                throw new ValidationException("User is not a regular customer, cannot redeem rewards");
            }
            requireRedeemable(merch);
            int pp = requirePointsPrice(merch);

            if (newQty == 0) {
                if (oldQty > 0) {
                    long refund = (long) pp * (long) oldQty;
                    if (refund > Integer.MAX_VALUE) {
                        throw new ValidationException("Points refund too large");
                    }
                    user.setRewardPoints(user.getRewardPoints() + (int) refund);
                    userRepository.save(user);
                }
            } else {
                int deltaQty = newQty - oldQty;
                if (deltaQty > 0) {
                    long extraCost = (long) pp * (long) deltaQty;
                    if (extraCost > Integer.MAX_VALUE) {
                        throw new ValidationException("Points cost too large");
                    }
                    if ((long) user.getRewardPoints() < extraCost) {
                        throw new ValidationException("Insufficient reward points");
                    }
                    user.setRewardPoints(user.getRewardPoints() - (int) extraCost);
                    userRepository.save(user);
                } else if (deltaQty < 0) {
                    long refund = (long) pp * (long) (-deltaQty);
                    if (refund > Integer.MAX_VALUE) {
                        throw new ValidationException("Points refund too large");
                    }
                    user.setRewardPoints(user.getRewardPoints() + (int) refund);
                    userRepository.save(user);
                }
            }
        }

        if (newQty == 0) {
            if (oldQty > 0) {
                merch.setRemainingQuantity(merch.getRemainingQuantity() + oldQty);
                merchandiseRepository.save(merch);
            }
            cartItemRepository.delete(item);
            return toDto(cartRepository.findById(cart.getId()).orElseThrow());
        }

        int delta = newQty - oldQty;

        if (delta > 0) {
            if (merch.getRemainingQuantity() < delta) {
                throw new ValidationException("Quantity for merchandise " + merch.getId() + " exceeds remaining quantity");
            }
            merch.setRemainingQuantity(merch.getRemainingQuantity() - delta);
            merchandiseRepository.save(merch);
        } else if (delta < 0) {
            merch.setRemainingQuantity(merch.getRemainingQuantity() + (-delta));
            merchandiseRepository.save(merch);
        }

        item.setQuantity(newQty);
        cartItemRepository.save(item);

        return toDto(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    @Override
    public void removeItem(String userEmail, Long cartItemId) {
        LOGGER.debug("Remove cart item: userEmail='{}', cartItemId={}", userEmail, cartItemId);
        if (cartItemId == null) {
            throw new ValidationException("CartItemId must be provided!");
        }
        User user = userService.findByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ValidationException("Cart not found for user!"));
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new NotFoundException("CartItem not found for cartItemId: " + cartItemId));
        assertOwnership(cart, item);

        if (item.getType() == CartItemType.MERCHANDISE || CartItemType.REWARD == item.getType()) {
            Merchandise merch = item.getMerchandise();
            Integer qty = item.getQuantity();
            if (merch == null || qty == null) {
                throw new ValidationException("Invalid cart state, merchandise is null for itemId: " + item.getId());
            }
            merch.setRemainingQuantity(merch.getRemainingQuantity() + qty);
            merchandiseRepository.save(merch);

            if (item.getType() == CartItemType.REWARD) {
                requireRedeemable(merch);
                int pp = requirePointsPrice(merch);
                long refund = (long) pp * (long) qty;
                if (refund > Integer.MAX_VALUE) {
                    throw new ValidationException("Points refund too large");
                }
                user.setRewardPoints(user.getRewardPoints() + (int) refund);
                userRepository.save(user);
            }
        }

        cartItemRepository.delete(item);
    }

    @Transactional
    @Override
    public CartCheckoutResultDto checkout(String userEmail, PaymentMethod paymentMethod, PaymentDetailDto paymentDetail) throws AccessDeniedException {
        LOGGER.debug("Checkout started: userEmail='{}', paymentMethod={}", userEmail, paymentMethod);
        User user = userService.findByEmail(userEmail);

        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ValidationException("Cart not found for user!"));

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());

        List<CartItem> merchItems = items.stream()
            .filter(i -> i.getType() == CartItemType.MERCHANDISE)
            .toList();

        List<CartItem> rewardItems = items.stream()
            .filter(i -> i.getType() == CartItemType.REWARD)
            .toList();

        List<CartItem> ticketItems = items.stream()
            .filter(i -> i.getType() == CartItemType.TICKET)
            .toList();

        if (merchItems.isEmpty() && rewardItems.isEmpty() && ticketItems.isEmpty()) {
            throw new ValidationException("Cart is empty");
        }

        Long merchInvoiceId = null;
        Long ticketInvoiceId = null;

        List<MerchandisePurchaseItemDto> purchaseItems = merchItems.stream()
            .map(i -> {
                if (i.getMerchandise() == null || i.getQuantity() == null || i.getQuantity() < 1) {
                    throw new ValidationException("Invalid cart state: " + i.getId());
                }
                return new MerchandisePurchaseItemDto(i.getMerchandise().getId(), i.getQuantity());
            })
            .toList();

        List<MerchandisePurchaseItemDto> rewardPurchaseItems = rewardItems.stream()
            .map(i -> {
                if (i.getMerchandise() == null || i.getQuantity() == null || i.getQuantity() < 1) {
                    throw new ValidationException("Invalid cart state: " + i.getId());
                }
                return new MerchandisePurchaseItemDto(i.getMerchandise().getId(), i.getQuantity());
            })
            .toList();

        if (!purchaseItems.isEmpty() || !rewardPurchaseItems.isEmpty()) {
            Invoice merchInvoice = invoiceService.purchaseMerchandiseWithRewards(
                user,
                purchaseItems,
                rewardPurchaseItems,
                paymentMethod,
                paymentDetail
            );
            merchInvoiceId = merchInvoice.getId();
        }

        if (!ticketItems.isEmpty()) {
            invoiceService.validatePaymentData(paymentMethod, paymentDetail);
            List<Long> ticketIds = ticketItems.stream()
                .map(CartItem::getTicketId)
                .toList();

            List<DetailedTicketDto> purchasedTickets = ticketService.purchase(ticketIds, userEmail);

            Invoice ticketInvoice = invoiceService.findById(purchasedTickets.get(0).invoiceId(), userEmail);
            ticketInvoiceId = ticketInvoice.getId();
        }

        cartItemRepository.deleteAll(items);

        return new CartCheckoutResultDto(merchInvoiceId, ticketInvoiceId);

    }

    @Transactional
    @Override
    public CartDto addTicket(String userEmail, Long ticketId) {
        LOGGER.debug("Add ticket to cart: userEmail='{}', ticketId={}", userEmail, ticketId);
        if (ticketId == null) {
            throw new ValidationException("TicketId must be provided!");
        }

        User user = userService.findByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseGet(() -> cartRepository.save(new Cart(user)));

        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("Ticket not found for ticketId: " + ticketId));

        if (ticket != null && ticket.getInvoice() != null) {
            throw new ValidationException("Tickets are already purchased!");
        }

        Reservation reservation = ticket.getReservation();
        User owner = reservation.getUser();
        if (owner == null || owner.getId() == null || !owner.getId().equals(user.getId())) {
            throw new ValidationException("Ticket does not belong to this user!");
        }



        CartItem item = cartItemRepository.findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, ticketId)
            .orElseGet(() -> {
                CartItem ci = new CartItem(CartItemType.TICKET);
                ci.setCart(cart);
                ci.setTicket(ticket);
                ci.setMerchandise(null);
                ci.setQuantity(null);
                return ci;
            });
        cartItemRepository.save(item);
        return toDto(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public List<CartDto> addTickets(String userEmail, List<Long> ticketIds) {
        LOGGER.debug("Add multiple tickets to cart: userEmail='{}', ticketIdsCount={}", userEmail, ticketIds != null ? ticketIds.size() : 0);
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new IllegalArgumentException("TicketIds must be provided!");
        }
        List<CartDto> addedCartItems = new ArrayList<>();
        ticketIds.forEach(ticketId -> {
            addedCartItems.add(addTicket(userEmail, ticketId));
        });
        return addedCartItems;
    }

    @Override
    public void removeTicket(String userEmail, Long ticketId) {
        LOGGER.debug("Remove ticket from cart: userEmail='{}', ticketId={}", userEmail, ticketId);
        if (ticketId == null) {
            throw new ValidationException("TicketId must be provided!");
        }

        User user = userService.findByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ValidationException("Cart not found for user!"));

        CartItem item = cartItemRepository
            .findByCartIdAndTypeAndTicket_Id(cart.getId(), CartItemType.TICKET, ticketId)
            .orElseThrow(() -> new NotFoundException("CartItem not found for ticketId: " + ticketId));

        assertOwnership(cart, item);
        Ticket ticket = item.getTicket();
        Reservation reservation = ticket.getReservation();

        cartItemRepository.delete(item);

        ticketRepository.delete(ticket);

        if (reservation != null) {
            long remainingTickets = ticketRepository.countByReservation(reservation);
            if (remainingTickets == 0) {
                reservationRepository.delete(reservation);
            }
        }
    }

    private CartDto toDto(Cart cart) {
        LOGGER.debug("Map cart to DTO: cartId={}", cart != null ? cart.getId() : null);
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        List<CartItemDto> dtos = cartItems.stream().map(i -> {
            if (i.getType() == CartItemType.MERCHANDISE) {
                Merchandise m = i.getMerchandise();
                Integer quantity = i.getQuantity();
                if (m == null || quantity == null) {
                    throw new ValidationException("Invalid merchandise cart item: " + i.getId());
                }
                return new CartItemDto(
                    i.getId(),
                    CartItemType.MERCHANDISE,
                    m.getId(),
                    m.getName(),
                    m.getUnitPrice(),
                    quantity,
                    m.getRemainingQuantity(),
                    m.hasImage(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            }

            if (i.getType() == CartItemType.REWARD) {
                Merchandise m = i.getMerchandise();
                Integer quantity = i.getQuantity();
                if (m == null || quantity == null) {
                    throw new ValidationException("Invalid reward cart item: " + i.getId());
                }
                return new CartItemDto(
                    i.getId(),
                    CartItemType.REWARD,
                    m.getId(),
                    m.getName(),
                    BigDecimal.ZERO,
                    quantity,
                    m.getRemainingQuantity(),
                    m.hasImage(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                );
            }

            if (i.getType() == CartItemType.TICKET) {
                if (i.getTicket() == null) {
                    throw new ValidationException("Invalid ticket cart item: " + i.getId());
                }
                var t = ticketRepository.findById(i.getTicket().getId())
                    .orElseThrow(() -> new NotFoundException("Ticket not found for ticketId: " + i.getTicket().getId()));

                String eventTitle = (t.getEvent() != null) ? t.getEvent().getTitle() : null;
                BigDecimal unitPrice = BigDecimal.valueOf(t.getGrossPrice());
                Long eventId = (t.getEvent() != null) ? t.getEvent().getId() : null;

                return new CartItemDto(
                    i.getId(),
                    CartItemType.TICKET,
                    null,
                    eventTitle,
                    unitPrice,
                    1,
                    null,
                    false,
                    t.getId(),
                    1,
                    eventId,
                    eventTitle,
                    i.getTicket().getSeat().getRowNumber(),
                    i.getTicket().getSeat().getSeatNumber()
                    );
            }

            throw new ValidationException("Invalid cart item type: " + i.getType());
        }).toList();

        BigDecimal merchTotal = cartItems.stream()
            .filter(i -> i.getType() == CartItemType.MERCHANDISE)
            .map(i -> {
                Merchandise m = i.getMerchandise();
                Integer quantity = i.getQuantity();
                if (m == null || quantity == null) {
                    throw new ValidationException("Invalid merchandise cart item: " + i.getId());
                }
                return m.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal ticketTotal = cartItems.stream().filter(i -> i.getType() == CartItemType.TICKET).map(i -> {
            if (i.getTicket() == null || i.getTicket().getGrossPrice() == null) {
                throw new ValidationException("Invalid ticket cart item: " + i.getId());
            }
            return BigDecimal.valueOf(i.getTicket().getGrossPrice());
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), dtos, merchTotal.add(ticketTotal));
    }

    private void assertOwnership(Cart cart, CartItem cartItem) {
        LOGGER.debug("Assert cart item ownership: cartId={}, cartItemId={}",
            cart != null ? cart.getId() : null, cartItem != null ? cartItem.getId() : null);
        if (cartItem.getCart() == null || cartItem.getCart().getId() == null || !cartItem.getCart().getId().equals(cart.getId())) {
            throw new ValidationException("Cart item does not belong to user's cart");
        }
    }

    private static final long REGULAR_USER_THRESHOLD_CENTS = 5000;

    private boolean isRegularCustomer(User user) {
        boolean regular = user.getTotalCentsSpent() >= REGULAR_USER_THRESHOLD_CENTS;
        LOGGER.debug("Check regular customer: userId={}, totalCentsSpent={}, threshold={}, isRegular={}",
            user.getId(), user.getTotalCentsSpent(), REGULAR_USER_THRESHOLD_CENTS, regular);
        return regular;
    }

    private int requirePointsPrice(Merchandise merch) {
        Integer pp = merch.getPointsPrice();
        LOGGER.debug("Require points price: merchandiseId={}, pointsPrice={}", merch.getId(), pp);
        if (pp == null || pp <= 0) {
            throw new ValidationException("Points price for merchandise " + merch.getId() + " must be positive");
        }
        return pp;
    }

    private void requireRedeemable(Merchandise merch) {
        LOGGER.debug("Require redeemable: merchandiseId={}, redeemableWithPoints={}", merch.getId(), merch.getRedeemableWithPoints());
        if (!Boolean.TRUE.equals(merch.getRedeemableWithPoints())) {
            throw new ValidationException("Merchandise " + merch.getId() + " is not redeemable with points");
        }
    }


}
