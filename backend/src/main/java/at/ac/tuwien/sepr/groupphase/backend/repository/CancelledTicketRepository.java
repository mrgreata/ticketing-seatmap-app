package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancelledTicketRepository extends JpaRepository<CancelledTicket, Long> {
    List<CancelledTicket> findByUser(User user);

    CancelledTicket save(CancelledTicket cancelledTicket);
}
