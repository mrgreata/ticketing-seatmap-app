package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.DetailedSectorDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SectorCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SimpleSectorDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SectorMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SeatMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
import at.ac.tuwien.sepr.groupphase.backend.service.SectorService;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatService;

import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sectors")
public class SectorEndpoint {

    private final SectorService sectorService;
    private final LocationService locationService;
    private final SeatService seatService;
    private final SectorMapper mapper;
    private final SeatMapper seatMapper;

    public SectorEndpoint(
        SectorService sectorService,
        LocationService locationService,
        SeatService seatService,
        SectorMapper mapper,
        SeatMapper seatMapper) {
        this.sectorService = sectorService;
        this.locationService = locationService;
        this.seatService = seatService;
        this.mapper = mapper;
        this.seatMapper = seatMapper;
    }

    @PermitAll
    @GetMapping("/{id}")
    public DetailedSectorDto findById(@PathVariable Long id) {
        var sector = sectorService.findById(id);

        var seats = seatService.findBySectorId(id).stream()
            .map(seatMapper::toSimple)
            .toList();

        return new DetailedSectorDto(
            sector.getId(),
            sector.getName(),
            sector.getLocation().getId(),
            seats
        );
    }

    @PermitAll
    @GetMapping("/location/{locationId}")
    public List<SimpleSectorDto> findByLocation(@PathVariable Long locationId) {
        return sectorService.findByLocationId(locationId).stream()
            .map(mapper::toSimple)
            .toList();
    }

    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleSectorDto create(@RequestBody SectorCreateDto dto) {
        var sector = mapper.fromCreateDto(dto);
        var location = locationService.findById(dto.locationId());
        sector.setLocation(location);

        var saved = sectorService.create(sector);
        return mapper.toSimple(saved);
    }
}