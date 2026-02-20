package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;

import java.util.List;

public interface SeatService {

    List<Seat> findAll();

    Seat findById(Long id);

    Seat create(Seat seat);

    List<Seat> findBySectorId(Long sectorId);
}