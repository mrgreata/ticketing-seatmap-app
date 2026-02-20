package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartAddMerchandiseItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutResultDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartUpdateItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.AccessDeniedException;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * REST endpoint for managing the shopping cart of the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CartService cartService;

    /**
     * Creates a new cart endpoint with the given cart service.
     *
     * @param cartService the cart service handling business logic
     */
    public CartEndpoint(CartService cartService) {
        this.cartService = cartService;
    }


    /**
     * Retrieves the current cart of the authenticated user.
     *
     * @param authentication the authentication object of the current user
     * @return the user's cart as a {@link CartDto}
     */
    @Secured("ROLE_USER")
    @GetMapping
    public CartDto getCart(Authentication authentication) {
        LOGGER.info("GET /api/v1/cart requested by user={}", authentication.getName());
        return cartService.getMyCart(authentication.getName());
    }

    /**
     * Adds a merchandise item to the authenticated user's cart.
     *
     * @param dto the request body containing merchandise ID, quantity,
     *            and reward point redemption flag
     * @param auth the authentication object of the current user
     * @return the updated cart as a {@link CartDto}
     */
    @Secured("ROLE_USER")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.OK)
    public CartDto addItem(@RequestBody CartAddMerchandiseItemDto dto, Authentication auth) {
        LOGGER.info("POST /api/v1/cart/items requested by user={} (merchandiseId={}, quantity={}, redeemedWithPoints={})",
            auth.getName(),
            dto != null ? dto.merchandiseId() : null,
            dto != null ? dto.quantity() : null,
            dto != null ? dto.redeemedWithPoints() : null
        );
        return cartService.addMerchandiseItem(auth.getName(), dto.merchandiseId(), dto.quantity(), dto.redeemedWithPoints());
    }

    /**
     * Updates the quantity of a merchandise item in the authenticated user's cart.
     *
     * @param cartItemId the ID of the cart item to update
     * @param dto the request body containing the new quantity
     * @param auth the authentication object of the current user
     * @return the updated cart as a {@link CartDto}
     */
    @Secured("ROLE_USER")
    @PatchMapping("/items/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CartDto updateQuantity(@PathVariable("id") Long cartItemId, @RequestBody CartUpdateItemDto dto, Authentication auth) {
        LOGGER.info("PATCH /api/v1/cart/items/{} requested by user={} (newQuantity={})",
            cartItemId,
            auth.getName(),
            dto != null ? dto.quantity() : null
        );
        return cartService.updateMerchandiseItemQuantity(auth.getName(), cartItemId, dto.quantity());
    }

    /**
     * Removes a merchandise item from the authenticated user's cart.
     *
     * @param cartItemId the ID of the cart item to remove
     * @param auth the authentication object of the current user
     */
    @Secured("ROLE_USER")
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable("id") Long cartItemId, Authentication auth) {
        LOGGER.info("DELETE /api/v1/cart/items/{} requested by user={}", cartItemId, auth.getName());
        cartService.removeItem(auth.getName(), cartItemId);
    }

    /**
     * Performs the checkout process for the authenticated user's cart.
     *
     * @param dto the checkout request containing payment method and details
     * @param auth the authentication object of the current user
     * @return the checkout result as a {@link CartCheckoutResultDto}
     * @throws AccessDeniedException
     *         if the user is not authorized to perform the checkout
     */
    @Secured("ROLE_USER")
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CartCheckoutResultDto checkout(@RequestBody CartCheckoutRequestDto dto, Authentication auth) throws AccessDeniedException {
        LOGGER.info("POST /api/v1/cart/checkout requested by user={}", auth.getName());
        return cartService.checkout(auth.getName(), dto.paymentMethod(), dto.paymentDetail());
    }

    /**
     * Adds multiple tickets to the authenticated user's cart.
     *
     * @param ticketIds the list of ticket IDs to add
     * @param auth the authentication object of the current user
     * @return a list of {@link CartDto} representing the cart state after each addition
     * @throws IllegalArgumentException
     *         if the list of ticket IDs is null or empty
     */
    @Secured("ROLE_USER")
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.OK)
    public List<CartDto> addTickets(@RequestBody List<Long> ticketIds, Authentication auth) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Ticket IDs dürfen nicht leer sein");
        }
        LOGGER.info("POST /api/v1/cart/tickets requested by user={} (ticketCount={})",
            auth.getName(),
            ticketIds != null ? ticketIds.size() : null
        );
        return cartService.addTickets(auth.getName(), ticketIds);
    }

    /**
     * Removes a ticket from the authenticated user's cart.
     *
     * @param ticketId the ID of the ticket to remove
     * @param auth the authentication object of the current user
     */
    @Secured("ROLE_USER")
    @DeleteMapping("/tickets/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTicket(@PathVariable("ticketId") Long ticketId, Authentication auth) {
        LOGGER.info("DELETE /api/v1/cart/tickets/{} requested by user={}", ticketId, auth.getName());
        cartService.removeTicket(auth.getName(), ticketId);
    }
}
