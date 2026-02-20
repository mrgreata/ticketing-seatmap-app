package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartCheckoutResultDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.cart.CartDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase.PaymentDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.type.PaymentMethod;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

/**
 * Service interface for managing the shopping cart of a user.
 */
public interface CartService {

    /**
     * Retrieves the current cart of the authenticated user.
     *
     * @param userEmail the email address identifying the user
     * @return the user's cart as a {@link CartDto}
     */
    CartDto getMyCart(String userEmail);

    /**
     * Adds a merchandise item to the user's cart.
     * If the item already exists in the cart, its quantity is increased
     * accordingly. Depending on the {@code redeemedWithPoints} flag, the item
     * is either purchased using reward points or regular payment.
     *
     * @param userEmail the email address identifying the user
     * @param merchandiseId the ID of the merchandise item to add
     * @param quantity the quantity to add
     * @param redeemedWitPoints whether the item is redeemed using reward points
     * @return the updated cart as a {@link CartDto}
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException
     *         if the quantity is invalid or reward points are insufficient
     */
    CartDto addMerchandiseItem(String userEmail, Long merchandiseId, int quantity, Boolean redeemedWitPoints);

    /**
     * Updates the quantity of a merchandise item already present in the cart.
     *
     * @param userEmail the email address identifying the user
     * @param cartItemId the ID of the cart item
     * @param quantity the new quantity to set
     * @return the updated cart as a {@link CartDto}
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException
     *         if the quantity is invalid
     */
    CartDto updateMerchandiseItemQuantity(String userEmail, Long cartItemId, int quantity);


    /**
     * Removes a cart item (merchandise) from the user's cart.
     *
     * @param userEmail the email address identifying the user
     * @param cartItemId the ID of the cart item to remove
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if the cart item does not exist
     */
    void removeItem(String userEmail, Long cartItemId);

    /**
     * Performs the checkout process for the user's cart.
     * This operation validates the cart contents, processes payment according
     * to the selected payment method, updates inventory and ticket states,
     * creates invoices, and clears the cart upon successful completion.
     *
     * @param userEmail the email address identifying the user
     * @param paymentMethod the selected payment method
     * @param paymentDetail additional payment details, if required
     * @return a {@link CartCheckoutResultDto} containing the checkout result
     * @throws AccessDeniedException
     *         if the user is not authorized to perform the checkout
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException
     *         if the cart is invalid or payment fails
     */
    CartCheckoutResultDto checkout(String userEmail, PaymentMethod paymentMethod, PaymentDetailDto paymentDetail) throws AccessDeniedException;

    /**
     * Adds a single ticket to the user's cart.
     *
     * @param userEmail the email address identifying the user
     * @param ticketId the ID of the ticket to add
     * @return the updated cart as a {@link CartDto}
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException
     *         if the ticket is already sold or reserved
     */
    CartDto addTicket(String userEmail, Long ticketId);

    /**
     * Adds multiple tickets to the user's cart in a single operation.
     *
     * @param userEmail the email address identifying the user
     * @param ticketId the list of ticket IDs to add
     * @return a list of {@link CartDto} representing the cart state after each addition
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException
     *         if one or more tickets cannot be added
     */
    List<CartDto> addTickets(String userEmail, List<Long> ticketId);

    /**
     * Removes a ticket from the user's cart.
     *
     * @param userEmail the email address identifying the user
     * @param ticketId the ID of the ticket to remove
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException
     *         if the ticket is not present in the cart
     */
    void removeTicket(String userEmail, Long ticketId);
}
