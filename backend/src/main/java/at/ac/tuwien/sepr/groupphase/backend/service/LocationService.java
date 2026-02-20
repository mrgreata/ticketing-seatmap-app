package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;

import java.util.List;

/**
 * Service for managing locations.
 */
public interface LocationService {

    /**
     * Find all locations.
     *
     * @return list of all locations
     */
    List<Location> findAll();

    /**
     * Find location by ID.
     *
     * @param id the location ID
     * @return the location entity
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if location not found
     */
    Location findById(Long id);

    /**
     * Create a new location.
     *
     * @param location the location to create
     * @return the created location
     */
    Location create(Location location);

    /**
     * Search locations by multiple criteria (all optional, case-insensitive partial match).
     * Combines all provided criteria with AND logic.
     *
     * @param name    the location name to search for (optional)
     * @param street  the street name to search for (optional)
     * @param city    the city name to search for (optional)
     * @param zipCode the exact zip code to match (optional)
     * @return list of matching locations
     */
    List<Location> searchLocations(String name, String street, String city, Integer zipCode);

    /**
     * Get location by ID with sectors as detailed DTO.
     *
     * @param id the location ID
     * @return the location as detailed DTO including all sectors
     * @throws at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException if location not found
     */
    DetailedLocationDto findByIdAsDto(Long id);

    /**
     * Create location from DTO.
     *
     * @param dto the location creation data
     * @return the created location as DTO
     */
    SimpleLocationDto createFromDto(LocationCreateDto dto);

    /**
     * Search locations by criteria and return as DTOs.
     * All criteria are optional and combined with AND logic.
     *
     * @param name    the location name (optional)
     * @param street  the street name (optional)
     * @param city    the city name (optional)
     * @param zipCode the zip code (optional)
     * @return list of matching locations as DTOs
     */
    List<SimpleLocationDto> searchLocationsAsDto(String name, String street, String city, Integer zipCode);

    /**
     * Get all locations as DTOs.
     *
     * @return list of all locations as DTOs
     */
    List<SimpleLocationDto> findAllAsDto();
}