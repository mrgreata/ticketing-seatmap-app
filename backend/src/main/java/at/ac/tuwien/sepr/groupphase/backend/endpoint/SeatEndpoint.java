package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SimpleSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.DetailedSeatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SeatMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatService;
import at.ac.tuwien.sepr.groupphase.backend.service.SectorService;

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
@RequestMapping("/api/v1/seats")
public class SeatEndpoint {

    private final SeatService seatService;
    private final SectorService sectorService;
    private final SeatMapper mapper;

    public SeatEndpoint(SeatService seatService, SectorService sectorService, SeatMapper mapper) {
        this.seatService = seatService;
        this.sectorService = sectorService;
        this.mapper = mapper;
    }

    @PermitAll
    @GetMapping("/{id}")
    public DetailedSeatDto findById(@PathVariable("id") Long id) {
        return mapper.toDetailed(seatService.findById(id));
    }

    @PermitAll
    @GetMapping("/sector/{sectorId}")
    public List<SimpleSeatDto> findBySector(@PathVariable Long sectorId) {
        return seatService.findBySectorId(sectorId).stream()
            .map(mapper::toSimple)
            .toList();
    }

    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleSeatDto create(@RequestBody SeatCreateDto dto) {
        var seat = mapper.fromCreateDto(dto);
        var sector = sectorService.findById(dto.sectorId());
        seat.setSector(sector);

        var saved = seatService.create(seat);
        return mapper.toSimple(saved);
    }
}