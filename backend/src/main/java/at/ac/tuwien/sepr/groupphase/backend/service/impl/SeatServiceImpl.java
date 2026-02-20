package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatService;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SeatMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SeatRepository seatRepository;

    public SeatServiceImpl(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public List<Seat> findAll() {
        LOGGER.debug("Find all seats");
        return seatRepository.findAll();
    }

    @Override
    public Seat findById(Long id) {
        LOGGER.debug("Find seat by id {}", id);
        return seatRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Seat not found: " + id));
    }

    @Override
    public Seat create(Seat seat) {
        LOGGER.debug("Create seat {}", seat);
        return seatRepository.save(seat);
    }

    @Override
    public List<Seat> findBySectorId(Long sectorId) {
        LOGGER.debug("Find seats by sectorId {}", sectorId);
        return seatRepository.findBySectorId(sectorId);
    }

}