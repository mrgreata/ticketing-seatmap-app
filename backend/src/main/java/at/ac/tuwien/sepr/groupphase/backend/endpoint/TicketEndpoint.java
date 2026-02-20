package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for managing tickets.
 * Provides operations for retrieving, purchasing, creating, and canceling tickets.
 * Also supports viewing canceled tickets.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketEndpoint.class);

    private final TicketService ticketService;

    public TicketEndpoint(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Returns all purchased tickets for the authenticated user.
     *
     * @param auth the current authenticated user
     * @return list of detailed tickets
     */
    @Secured("ROLE_USER")
    @GetMapping("/my")
    public List<DetailedTicketDto> getMyPurchasedTickets(Authentication auth) {
        LOGGER.info("Fetching purchased tickets for user {}", auth.getName());
        return ticketService.getMyPurchasedTickets(auth.getName());
    }

    /**
     * Retrieves a single ticket by ID for the authenticated user.
     *
     * @param id   the ticket ID
     * @param auth the current authenticated user
     * @return detailed ticket DTO
     * @throws AccessDeniedException if user is not authorized to view the ticket
     */
    @Secured("ROLE_USER")
    @GetMapping("/{id}")
    public DetailedTicketDto findById(@PathVariable("id") Long id, Authentication auth) throws AccessDeniedException {
        LOGGER.info("Fetching ticket {} for user {}", id, auth.getName());
        return ticketService.getTicketByIdForUser(id, auth.getName());
    }

    /**
     * Purchases tickets for the authenticated user.
     *
     * @param ticketIds list of ticket IDs to purchase
     * @param auth      the current authenticated user
     * @return list of updated detailed tickets
     */
    @Secured("ROLE_USER")
    @PatchMapping("/purchasing")
    @ResponseStatus(HttpStatus.OK)
    public List<DetailedTicketDto> purchase(@RequestBody List<Long> ticketIds, Authentication auth) throws AccessDeniedException {
        LOGGER.info("Purchasing tickets {} for user {}", ticketIds, auth.getName());
        return ticketService.purchase(ticketIds, auth.getName());
    }

    /**
     * Cancels tickets and creates a credit invoice for the user.
     *
     * @param ticketIds list of ticket IDs to cancel
     * @param auth      the current authenticated user
     */
    @Secured("ROLE_USER")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTickets(@RequestBody List<Long> ticketIds, Authentication auth) throws AccessDeniedException {
        LOGGER.info("Canceling tickets {} for user {}", ticketIds, auth.getName());
        ticketService.createCreditInvoice(ticketIds, auth.getName());
    }

    /**
     * Creates new tickets for the authenticated user.
     *
     * @param tickets list of ticket creation DTOs
     * @param auth    the current authenticated user
     * @return list of created detailed tickets
     */
    @Secured("ROLE_USER")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<DetailedTicketDto> create(@RequestBody List<TicketCreateDto> tickets, Authentication auth) {
        LOGGER.info("Creating tickets {} for user {}", tickets, auth.getName());
        return ticketService.create(tickets, auth.getName());
    }

    /**
     * Returns canceled tickets for the authenticated user.
     *
     * @param auth the current authenticated user
     * @return list of canceled tickets
     */
    @GetMapping("/cancelled/my")
    public ResponseEntity<List<CancelledTicketDto>> getMyCancelledTickets(Authentication auth) {
        LOGGER.info("Fetching canceled tickets for user {}", auth.getName());
        List<CancelledTicketDto> tickets = ticketService.getMyCancelledTickets(auth.getName());
        return ResponseEntity.ok(tickets);
    }
}
