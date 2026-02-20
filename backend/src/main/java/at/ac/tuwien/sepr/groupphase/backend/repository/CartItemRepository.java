package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.CartItem;
import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing {@link CartItem} entities.
 * Cart items can represent different item types, such as merchandise, rewards
 * or tickets, which are distinguished using {@link CartItemType}.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Retrieves all cart items belonging to a specific cart.
     *
     * @param cartId the ID of the cart
     * @return a list of {@link CartItem} associated with the cart
     */
    List<CartItem> findAllByCartId(Long cartId);

    /**
     * Retrieves a merchandise cart item for a given cart.
     * This method is typically used to check whether a specific
     * merchandise item already exists in the cart.
     *
     * @param cartId the ID of the cart
     * @param cartItemType the type of the cart item (e.g. MERCHANDISE)
     * @param merchandiseId the ID of the merchandise item
     * @return an {@link Optional} containing the cart item if found
     */
    Optional<CartItem> findByCartIdAndTypeAndMerchandiseId(Long cartId, CartItemType cartItemType, Long merchandiseId);

    /**
     * Retrieves a ticket cart item for a given cart.
     * This method is typically used to check whether a specific
     * ticket is already present in the cart.
     *
     * @param cartId the ID of the cart
     * @param cartItemType the type of the cart item (e.g. TICKET)
     * @param ticketId the ID of the ticket
     * @return an {@link Optional} containing the cart item if found
     */
    Optional<CartItem> findByCartIdAndTypeAndTicket_Id(Long cartId, CartItemType cartItemType, Long ticketId);
}
