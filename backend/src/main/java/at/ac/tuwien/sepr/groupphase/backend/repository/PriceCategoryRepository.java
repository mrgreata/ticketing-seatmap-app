package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceCategoryRepository extends JpaRepository<PriceCategory, Long> {

    List<PriceCategory> findBySectorId(Long id);
}