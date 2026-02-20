package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.SectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class SectorServiceImpl implements SectorService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SectorRepository sectorRepository;

    public SectorServiceImpl(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Override
    public List<Sector> findAll() {
        LOGGER.debug("Find all sectors");
        return sectorRepository.findAll();
    }

    @Override
    public Sector findById(Long id) {
        LOGGER.debug("Find sector by id {}", id);
        return sectorRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Sector not found: " + id));
    }

    @Override
    public Sector create(Sector sector) {
        LOGGER.debug("Create sector {}", sector);
        return sectorRepository.save(sector);
    }

    @Override
    public List<Sector> findByLocationId(Long locationId) {
        LOGGER.debug("Find sectors by locationId {}", locationId);
        return sectorRepository.findByLocationId(locationId);
    }
}