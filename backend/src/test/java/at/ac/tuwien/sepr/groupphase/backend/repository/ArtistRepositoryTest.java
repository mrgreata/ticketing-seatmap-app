package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ArtistRepository CUSTOM @Query methods only.
 */
@DataJpaTest
class ArtistRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArtistRepository artistRepository;

    private Artist bandMember1;
    private Artist bandMember2;
    private Artist band;

    @BeforeEach
    void setUp() {
        bandMember1 = new Artist();
        bandMember1.setName("Freddie Mercury");
        bandMember1.setIsBand(false);
        entityManager.persist(bandMember1);

        bandMember2 = new Artist();
        bandMember2.setName("Brian May");
        bandMember2.setIsBand(false);
        entityManager.persist(bandMember2);

        band = new Artist();
        band.setName("Queen");
        band.setIsBand(true);
        band.setMembers(List.of(bandMember1, bandMember2));
        entityManager.persist(band);

        entityManager.flush();
    }

    @Test
    void findBandsByMemberId_findsQueenForFreddie() {
        List<Artist> results = artistRepository.findBandsByMemberId(bandMember1.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Queen");
        assertThat(results.get(0).getIsBand()).isTrue();
    }

    @Test
    void findBandsByMemberId_findsQueenForBrian() {
        List<Artist> results = artistRepository.findBandsByMemberId(bandMember2.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Queen");
    }

    @Test
    void findBandsByMemberId_returnsEmptyForNonMember() {
        Artist solo = new Artist();
        solo.setName("Ed Sheeran");
        solo.setIsBand(false);
        entityManager.persist(solo);
        entityManager.flush();

        List<Artist> results = artistRepository.findBandsByMemberId(solo.getId());

        assertThat(results).isEmpty();
    }

    @Test
    void findBandsByMemberId_returnsMultipleBands() {
        Artist anotherBand = new Artist();
        anotherBand.setName("The Highlanders");
        anotherBand.setIsBand(true);
        anotherBand.setMembers(List.of(bandMember1));
        entityManager.persist(anotherBand);
        entityManager.flush();

        List<Artist> results = artistRepository.findBandsByMemberId(bandMember1.getId());

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(Artist::getIsBand);
    }
}