package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for accessing {@link Merchandise} entities.
 */
@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Long> {


    /**
     * Retrieves all merchandise items that can be redeemed using reward points.
     * Only non-deleted merchandise items are returned.
     *
     * @return a list of reward-eligible {@link Merchandise} entities
     */
    List<Merchandise> findByRedeemableWithPointsTrueAndDeletedFalse();


    /**
     * Retrieves all non-deleted merchandise items.
     *
     * @return a list of active {@link Merchandise} entities
     */
    List<Merchandise> findAllByDeletedFalse();
}