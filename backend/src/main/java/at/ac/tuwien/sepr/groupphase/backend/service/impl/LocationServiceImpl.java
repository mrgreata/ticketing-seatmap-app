package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.LocationMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SectorMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;

import at.ac.tuwien.sepr.groupphase.backend.service.SectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass());
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final SectorMapper sectorMapper;
    private final SectorService sectorService;

    public LocationServiceImpl(LocationRepository locationRepository,
                               LocationMapper locationMapper,
                               SectorMapper sectorMapper,
                               SectorService sectorService) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
        this.sectorMapper = sectorMapper;
        this.sectorService = sectorService;
    }

    @Override
    public List<Location> findAll() {
        LOGGER.debug("Find all locations");
        return locationRepository.findAll();
    }

    @Override
    public Location findById(Long id) {
        LOGGER.debug("Find location by id {}", id);
        return locationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Location not found: " + id));
    }

    @Override
    public Location create(Location location) {
        LOGGER.debug("Create location {}", location);
        return locationRepository.save(location);
    }

    @Override
    public List<Location> searchLocations(String name, String street, String city, Integer zipCode) {
        LOGGER.debug("Search locations: name={}, street={}, city={}, zipCode={}", name, street, city, zipCode);
        return locationRepository.searchLocations(name, street, city, zipCode);
    }

    @Override
    public DetailedLocationDto findByIdAsDto(Long id) {
        LOGGER.debug("Find location by id {} as detailed DTO", id);

        var location = findById(id);
        var sectors = sectorService.findAll()
            .stream()
            .filter(s -> s.getLocation().getId().equals(id))
            .map(sectorMapper::toSimple)
            .toList();

        return new DetailedLocationDto(
            location.getId(),
            location.getName(),
            location.getZipCode(),
            location.getCity(),
            location.getStreet(),
            location.getStreetNumber(),
            sectors,

            location.getStagePosition(),
            location.getStageLabel(),
            location.getStageHeightPx(),
            location.getStageWidthPx(),
            location.getStageRowStart(),
            location.getStageRowEnd(),
            location.getStageColStart(),
            location.getStageColEnd(),

            location.getRunwayWidthPx(),
            location.getRunwayLengthPx(),
            location.getRunwayOffsetPx()
        );
    }

    @Override
    public SimpleLocationDto createFromDto(LocationCreateDto dto) {
        LOGGER.debug("Create location from DTO: {}", dto.name());
        var entity = locationMapper.fromCreateDto(dto);
        var saved = create(entity);
        return locationMapper.toSimple(saved);
    }

    @Override
    public List<SimpleLocationDto> searchLocationsAsDto(String name, String street, String city, Integer zipCode) {
        LOGGER.debug("Search locations as DTOs: name={}, street={}, city={}, zipCode={}", name, street, city, zipCode);
        return searchLocations(name, street, city, zipCode)
            .stream()
            .map(locationMapper::toSimple)
            .toList();
    }

    @Override
    public List<SimpleLocationDto> findAllAsDto() {
        LOGGER.debug("Find all locations as DTOs");
        return findAll()
            .stream()
            .map(locationMapper::toSimple)
            .toList();
    }
}