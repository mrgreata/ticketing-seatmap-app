package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

public interface ReservationService {

    List<Reservation> findAll();

    SimpleReservationDto findById(Long id, String userEmail) throws AccessDeniedException;

    SimpleReservationDto create(List<Long> ticketIds, String userEmail);

    List<DetailedReservationDto> findByUser(String userEmail);

    void cancelReservations(List<Long> ticketIds, String userEmail) throws AccessDeniedException;
}
