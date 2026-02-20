package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;

import java.util.List;

/**
 * Service for managing artists.
 */
public interface ArtistService {

    /**
     * Find all artists.
     *
     * @return list of all artists
     */
    List<Artist> findAll();

    /**
     * Find artist by ID.
     *
     * @param id the artist ID
     * @return the artist entity
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if artist not found
     */
    Artist findById(Long id);

    /**
     * Create a new artist.
     *
     * @param artist the artist to create
     * @return the created artist
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if artist data is invalid
     */
    Artist create(Artist artist);

    /**
     * Search artists by name (case-insensitive, partial match).
     *
     * @param name the name to search for
     * @return list of matching artists
     */
    List<Artist> searchByName(String name);

    /**
     * Search artists by name and include bands where the artist is a member.
     * This returns both direct matches and any bands containing the artist.
     *
     * @param name the name to search for
     * @return list of matching artists and their bands
     */
    List<Artist> searchArtistsWithBands(String name);

    /**
     * Create artist from DTO including member resolution for bands.
     * If the artist is a band, member IDs are resolved to actual artist entities.
     *
     * @param dto the artist creation data
     * @return the created artist as DTO
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException if data is invalid
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException   if member artist not found
     */
    SimpleArtistDto createFromDto(ArtistCreateDto dto);

    /**
     * Search artists by name and optionally include bands.
     * Returns DTOs with all mapping already applied.
     *
     * @param name         the name to search for
     * @param includeBands if true, includes bands where artist is a member
     * @return list of matching artists as DTOs
     */
    List<SimpleArtistDto> searchArtistsAsDto(String name, boolean includeBands);

    /**
     * Get all artists as DTOs.
     *
     * @return list of all artists as DTOs
     */
    List<SimpleArtistDto> findAllAsDto();

    /**
     * Get artist by ID as detailed DTO including band members.
     *
     * @param id the artist ID
     * @return the artist as detailed DTO
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if artist not found
     */
    DetailedArtistDto findByIdAsDto(Long id);
}