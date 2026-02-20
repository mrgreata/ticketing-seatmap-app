package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceMerchandiseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceMerchandiseItemRepository extends JpaRepository<InvoiceMerchandiseItem, Long> {
}
