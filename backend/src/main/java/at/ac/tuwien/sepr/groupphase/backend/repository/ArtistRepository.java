package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Artist} entities.
 */
@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /**
     * Find artists by name containing search term (case-insensitive).
     * Supports both band names and individual artist names.
     *
     * @param name the search term
     * @return list of matching artists
     */
    List<Artist> findByNameContainingIgnoreCase(String name);

    /**
     * Find all bands where the given artist is a member.
     * Only returns artists where {@code isBand = true}.
     *
     * @param artistId the artist ID to search for in band members
     * @return list of bands containing this artist
     */
    @Query("SELECT a FROM Artist a JOIN a.members m WHERE m.id = :artistId AND a.isBand = true")
    List<Artist> findBandsByMemberId(@Param("artistId") Long artistId);
}