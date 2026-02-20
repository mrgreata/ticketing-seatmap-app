package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LocationRepository CUSTOM @Query methods only.
 */
@DataJpaTest
class LocationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LocationRepository locationRepository;

    private Location stadthalle;
    private Location staatsoper;

    @BeforeEach
    void setUp() {
        stadthalle = new Location();
        stadthalle.setName("Wiener Stadthalle");
        stadthalle.setStreet("Roland-Rainer-Platz");
        stadthalle.setStreetNumber("1");
        stadthalle.setCity("Wien");
        stadthalle.setZipCode(1150);
        stadthalle.setStagePosition("TOP");
        entityManager.persist(stadthalle);

        staatsoper = new Location();
        staatsoper.setName("Wiener Staatsoper");
        staatsoper.setStreet("Opernring");
        staatsoper.setStreetNumber("2");
        staatsoper.setCity("Wien");
        staatsoper.setZipCode(1010);
        staatsoper.setStagePosition("TOP");
        entityManager.persist(staatsoper);

        entityManager.flush();
    }

    @Test
    void searchLocations_combinesAllCriteria() {
        List<Location> results = locationRepository.searchLocations(
            "stadthalle", "roland", "wien", 1150
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Wiener Stadthalle");
    }

    @Test
    void searchLocations_allNullReturnsAll() {
        List<Location> results = locationRepository.searchLocations(
            null, null, null, null
        );

        assertThat(results).hasSize(2);
    }

    @Test
    void searchLocations_partialMatch() {
        List<Location> results = locationRepository.searchLocations(
            "wiener", null, null, null
        );

        assertThat(results).hasSize(2);
    }

    @Test
    void searchLocations_noMatch() {
        List<Location> results = locationRepository.searchLocations(
            "non-existent", null, null, null
        );

        assertThat(results).isEmpty();
    }
}