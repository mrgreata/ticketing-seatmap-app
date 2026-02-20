package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEventIdAndSeatIdIn(Long eventId, List<Long> seatIds);

    long countByReservation(Reservation reservation);

    boolean existsByEventIdAndSeatId(Long eventId, Long seatId);

    List<Ticket> findByInvoiceIsNull();

    List<Ticket> findByInvoiceIsNullAndReservationIsNull();
}