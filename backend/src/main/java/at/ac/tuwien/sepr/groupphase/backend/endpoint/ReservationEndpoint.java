package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationEndpoint {

    private final ReservationService reservationService;

    public ReservationEndpoint(
        ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    /**
     * Returns the current user's reservations.
     */
    @Secured("ROLE_USER")
    @GetMapping("/my")
    public List<DetailedReservationDto> getMyReservations(Authentication auth) {
        return reservationService.findByUser(auth.getName());
    }

    /**
     * Returns a reservation by id (access-controlled via service).
     */
    @Secured("ROLE_USER")
    @GetMapping("/{id}")
    public SimpleReservationDto findById(@PathVariable("id") Long id, Authentication auth) throws AccessDeniedException {
        return reservationService.findById(id, auth.getName());
    }

    /**
     * Reserve tickets: creates a reservation for the given ticketIds.
     */
    @Secured("ROLE_USER")
    @PatchMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleReservationDto reserve(@RequestBody List<Long> ticketIds, Authentication auth) {
        return reservationService.create(ticketIds, auth.getName());
    }

    /**
     * Cancel reservations for the given ticketIds.
     */
    @Secured("ROLE_USER")
    @PatchMapping("/cancellation")
    @ResponseStatus(HttpStatus.OK)
    public void cancelReservations(@RequestBody List<Long> ticketIds, Authentication auth) throws AccessDeniedException {
        reservationService.cancelReservations(ticketIds, auth.getName());
    }

}
