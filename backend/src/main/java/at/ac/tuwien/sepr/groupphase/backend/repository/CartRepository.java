package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for accessing {@link Cart} entities.
 * Each user is expected to have at most one active cart at a time.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Retrieves the cart associated with a specific user.
     *
     * @param id the ID of the user
     * @return an {@link Optional} containing the user's cart if present
     */
    Optional<Cart> findByUserId(Long id);
}
