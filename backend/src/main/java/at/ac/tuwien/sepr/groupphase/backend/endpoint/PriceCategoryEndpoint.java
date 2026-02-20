package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.PriceCategoryCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.SimplePriceCategoryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.priceCategory.DetailedPriceCategoryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.PriceCategoryMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.PriceCategoryService;
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
@RequestMapping("/api/v1/price-categories")
public class PriceCategoryEndpoint {

    private final PriceCategoryService priceCategoryService;
    private final SectorService sectorService;
    private final PriceCategoryMapper mapper;

    public PriceCategoryEndpoint(
        PriceCategoryService priceCategoryService,
        SectorService sectorService,
        PriceCategoryMapper mapper) {
        this.priceCategoryService = priceCategoryService;
        this.sectorService = sectorService;
        this.mapper = mapper;
    }

    @PermitAll
    @GetMapping
    public List<SimplePriceCategoryDto> findAll() {
        return priceCategoryService.findAll().stream()
            .map(mapper::toSimple)
            .toList();
    }

    @PermitAll
    @GetMapping("/{id}")
    public DetailedPriceCategoryDto findById(@PathVariable Long id) {
        return mapper.toDetailed(priceCategoryService.findById(id));
    }

    @PermitAll
    @GetMapping("/sector/{sectorId}")
    public List<SimplePriceCategoryDto> findBySector(@PathVariable Long sectorId) {
        return priceCategoryService.findAll().stream()
            .filter(pc -> pc.getSector().getId().equals(sectorId))
            .map(mapper::toSimple)
            .toList();
    }

    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimplePriceCategoryDto create(@RequestBody PriceCategoryCreateDto dto) {
        var entity = mapper.fromCreateDto(dto);
        entity.setSector(sectorService.findById(dto.sectorId()));

        var saved = priceCategoryService.create(entity);
        return mapper.toSimple(saved);
    }
}