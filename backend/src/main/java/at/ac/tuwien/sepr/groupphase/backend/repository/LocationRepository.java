package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Location} entities.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    /**
     * Search locations by multiple optional criteria.
     * All criteria are combined with AND logic and use case-insensitive partial matching.
     * Null parameters are ignored.
     *
     * @param name    the location name (optional)
     * @param street  the street name (optional)
     * @param city    the city name (optional)
     * @param zipCode the exact zip code (optional)
     * @return list of matching locations
     */
    @Query("SELECT l FROM Location l WHERE "
        + "(:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND "
        + "(:street IS NULL OR LOWER(l.street) LIKE LOWER(CONCAT('%', :street, '%'))) AND "
        + "(:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND "
        + "(:zipCode IS NULL OR l.zipCode = :zipCode)")
    List<Location> searchLocations(
        @Param("name") String name,
        @Param("street") String street,
        @Param("city") String city,
        @Param("zipCode") Integer zipCode
    );
}