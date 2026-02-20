package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;

import java.util.List;

public interface SectorService {

    List<Sector> findAll();

    Sector findById(Long id);

    Sector create(Sector sector);

    List<Sector> findByLocationId(Long locationId);
}