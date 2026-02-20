
package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

/**
 * Service for managing tickets, including creation, purchase, reservation, and cancellation.
 */
public interface TicketService {

    /**
     * Retrieves all tickets.
     *
     * @return List of all tickets.
     */
    List<Ticket> findAll();

    /**
     * Retrieves all purchased tickets of a user in detailed format.
     *
     * @param userEmail Email of the user.
     * @return List of DetailedTicketDto.
     */
    List<DetailedTicketDto> getMyPurchasedTickets(String userEmail);

    /**
     * Retrieves all cancelled tickets of a user.
     *
     * @param userEmail Email of the user.
     * @return List of CancelledTicketDto.
     */
    List<CancelledTicketDto> getMyCancelledTickets(String userEmail);

    /**
     * Finds a ticket by ID.
     *
     * @param id Ticket ID.
     * @return Ticket entity.
     */
    Ticket findById(Long id);

    /**
     * Creates new tickets.
     *
     * @param ticketsToCreate List of DTOs representing tickets to create.
     * @param userEmail Email of the user creating tickets.
     * @return List of DetailedTicketDto for created tickets.
     */
    List<DetailedTicketDto> create(List<TicketCreateDto> ticketsToCreate, String userEmail);

    /**
     * Saves a ticket entity.
     *
     * @param ticket Ticket to save.
     */
    void save(Ticket ticket);

    /**
     * Finds multiple tickets by their IDs.
     *
     * @param ids List of ticket IDs.
     * @return List of Ticket entities.
     */
    List<Ticket> findAllByIds(List<Long> ids);

    /**
     * Deletes a ticket by its ID.
     *
     * @param id Ticket ID to delete.
     */
    void deleteById(Long id);

    /**
     * Purchases tickets for a user, generating an invoice.
     *
     * @param ticketIds List of ticket IDs to purchase.
     * @param userEmail Email of the user purchasing tickets.
     * @return List of DetailedTicketDto of purchased tickets.
     * @throws AccessDeniedException if the user does not own the tickets.
     */
    List<DetailedTicketDto> purchase(List<Long> ticketIds, String userEmail) throws AccessDeniedException;

    /**
     * Reserves tickets for a reservation.
     *
     * @param reservation Reservation entity.
     * @param tickedIds List of ticket IDs to reserve.
     * @param userEmail Email of the user.
     * @return List of DetailedTicketDto of reserved tickets.
     */
    List<DetailedTicketDto> reserve(Reservation reservation, List<Long> tickedIds, String userEmail);

    /**
     * Deletes multiple tickets by their IDs.
     *
     * @param ids List of ticket IDs to delete.
     * @param userEmail Email of the user.
     * @throws AccessDeniedException if the user does not own the tickets.
     */
    void deleteByIds(List<Long> ids, String userEmail) throws AccessDeniedException;

    /**
     * Creates cancelled tickets for a credit invoice.
     *
     * @param ticketIds List of ticket IDs to cancel.
     * @param userEmail Email of the user.
     * @param creditInvoice Credit invoice entity.
     * @return List of CancelledTicket entities.
     */
    List<CancelledTicket> createCancelledTickets(List<Long> ticketIds, String userEmail, Invoice creditInvoice);

    /**
     * Retrieves detailed information of a ticket for a user.
     *
     * @param id Ticket ID.
     * @param userEmail Email of the user.
     * @return DetailedTicketDto.
     * @throws AccessDeniedException if the user does not own the ticket.
     */
    DetailedTicketDto getTicketByIdForUser(Long id, String userEmail) throws AccessDeniedException;

    /**
     * Creates a credit invoice for specific tickets.
     *
     * @param ticketIds List of ticket IDs to include in the credit invoice.
     * @param userEmail Email of the user.
     * @throws AccessDeniedException if the user does not own the tickets.
     */
    void createCreditInvoice(List<Long> ticketIds, String userEmail) throws AccessDeniedException;

}