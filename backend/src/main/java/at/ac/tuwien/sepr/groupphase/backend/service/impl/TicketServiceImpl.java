
package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.CancelledTicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.TicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.CancelledTicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.ReservationService;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;

import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;

import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TicketRepository ticketRepository;

    @Lazy
    private final InvoiceService invoiceService;

    private final UserService userService;
    private final SeatService seatService;
    private final EventServiceImpl eventService;
    private final CancelledTicketRepository cancelledTicketRepository;
    private final CancelledTicketMapper cancelledTicketMapper;
    private final TicketMapper ticketMapper;
    private final ReservationRepository reservationRepository;


    public TicketServiceImpl(TicketRepository ticketRepository, @Lazy InvoiceService invoiceService, UserService userService, SeatService seatService, EventServiceImpl eventService,
                             CancelledTicketRepository cancelledTicketRepository, CancelledTicketMapper cancelledTicketMapper, TicketMapper ticketMapper,
                             ReservationRepository reservationRepository) {
        this.ticketRepository = ticketRepository;
        this.invoiceService = invoiceService;
        this.userService = userService;
        this.seatService = seatService;
        this.eventService = eventService;
        this.cancelledTicketRepository = cancelledTicketRepository;
        this.cancelledTicketMapper = cancelledTicketMapper;
        this.ticketMapper = ticketMapper;
        this.reservationRepository = reservationRepository;
    }


    /**
     * Find user by email or throw NotFoundException.
     */
    private User findUserOrThrow(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found: " + email);
        }
        return user;
    }

    /**
     * Finds a ticket or throws if not found.
     */
    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    /**
     * Ensures that the given ticket belongs to the given user
     * either via reservation or via invoice.
     */
    private void verifyTicketOwnership(Ticket ticket, String userEmail) throws AccessDeniedException {
        boolean ownsReservation = ticket.getReservation() != null
            && ticket.getReservation().getUser() != null
            && userEmail.equals(ticket.getReservation().getUser().getEmail());

        boolean ownsInvoice = ticket.getInvoice() != null
            && ticket.getInvoice().getUser() != null
            && userEmail.equals(ticket.getInvoice().getUser().getEmail());

        boolean isUnassigned = ticket.getInvoice() == null && ticket.getReservation() == null;

        if (!ownsReservation && !ownsInvoice && !isUnassigned) {
            throw new AccessDeniedException("Access denied for ticket: " + ticket.getId());
        }
    }


    @Override
    public List<Ticket> findAll() {
        LOGGER.debug("Find all tickets");
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public List<DetailedTicketDto> getMyPurchasedTickets(String userEmail) {
        List<DetailedInvoiceDto> invoices = invoiceService.getMyInvoices(userEmail);

        List<Long> ticketIds = invoices.stream()
            .flatMap(i -> i.tickets().stream())
            .map(t -> t.id())
            .toList();

        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        return tickets.stream()
            .map(ticketMapper::toDetailed)
            .toList();
    }


    @Override
    public List<CancelledTicketDto> getMyCancelledTickets(String userEmail) {
        User user = findUserOrThrow(userEmail);
        List<CancelledTicket> cancelledTickets = cancelledTicketRepository.findByUser(user);
        LOGGER.info("getMyCancelledTickets Service: {}", cancelledTickets);

        return cancelledTickets.stream()
            .map(cancelledTicketMapper::toDto)
            .toList();
    }


    @Override
    public Ticket findById(Long id) {
        LOGGER.debug("Find ticket by id {}", id);
        return ticketRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }


    @Override
    public DetailedTicketDto getTicketByIdForUser(Long ticketId, String userEmail) throws AccessDeniedException {
        Ticket ticket = findTicketOrThrow(ticketId);
        verifyTicketOwnership(ticket, userEmail);
        return ticketMapper.toDetailed(ticket);
    }


    @Override
    public void createCreditInvoice(List<Long> ticketIds, String userEmail) throws AccessDeniedException {
        invoiceService.createCreditInvoice(ticketIds, userEmail);
    }


    @Transactional
    @Override
    public List<DetailedTicketDto> create(List<TicketCreateDto> ticketsToCreate, String userEmail) {

        findUserOrThrow(userEmail);
        if (ticketsToCreate.isEmpty()) {
            throw new IllegalArgumentException("Cannot create empty tickets");
        }
        Set<String> duplicatesInRequest = new HashSet<>();
        for (TicketCreateDto dto : ticketsToCreate) {
            String key = dto.eventId() + "-" + dto.seatId();
            if (!duplicatesInRequest.add(key)) {
                throw new ValidationException(
                    "Duplicate ticket in request for event " + dto.eventId() + " and seat " + dto.seatId()
                );
            }
        }


        List<Ticket> tickets = ticketsToCreate.stream()
            .map(dto -> {
                Ticket ticket = ticketMapper.fromCreateDto(dto);

                Event event = eventService.findById(dto.eventId());
                Seat seat = seatService.findById(dto.seatId());

                if (ticketRepository.existsByEventIdAndSeatId(event.getId(), seat.getId())) {
                    throw new ValidationException(
                        "A ticket for event " + event.getTitle()
                            +
                            " and seat "
                            + seat.getSeatNumber()
                            + " already exists"
                    );
                }

                ticket.setEvent(event);
                ticket.setSeat(seat);
                ticket.setSector(seat.getSector());
                ticket.setLocation(event.getLocation());

                double base = seat.getPriceCategory().getBasePrice() / 100.0;
                ticket.setNetPrice(base);
                ticket.setTaxRate(0.2);
                ticket.setGrossPrice(base * 1.2);

                return ticket;
            })
            .toList();
        List<Ticket> savedTickets;

        try {
            savedTickets = ticketRepository.saveAll(tickets);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException(
                "Seat already booked for this event",
                List.of("One of the selected seats was booked in the meantime."),
                ex
            );
        }        // 4️⃣ Lazy-Proxies auflösen, damit MapStruct IDs korrekt ausliest
        savedTickets.forEach(ticket -> {
            if (ticket.getEvent() != null) {
                ticket.getEvent().getId();
            }
            if (ticket.getSeat() != null) {
                ticket.getSeat().getId();
            }
            if (ticket.getSector() != null) {
                ticket.getSector().getId();
            }
            if (ticket.getLocation() != null) {
                ticket.getLocation().getId();
            }
        });
        return savedTickets.stream().map(ticketMapper::toDetailed).toList();

    }


    @Override
    public void save(Ticket ticket) {
        ticketRepository.save(ticket);
    }


    @Override
    @EntityGraph(attributePaths = {"event", "seat", "sector", "location"})
    public List<Ticket> findAllByIds(List<Long> ids) {
        return ticketRepository.findAllById(ids);
    }

    @Override
    public void deleteById(Long id) {
        ticketRepository.deleteById(id);
    }

    @Transactional
    @Override
    public List<DetailedTicketDto> purchase(List<Long> ticketIds, String userEmail) throws AccessDeniedException {

        if (ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Cannot purchase empty tickets");
        }
        User user = findUserOrThrow(userEmail);

        for (Long id : ticketIds) {
            Ticket ticket = findTicketOrThrow(id);
            if (ticket.getInvoice() != null) {
                throw new ValidationException("Cannot purchase ticket (already purchased) " + ticket.getInvoice());
            }
            verifyTicketOwnership(ticket, userEmail);
        }


        LOGGER.info("Purchasing tickets for " + user.getId());


        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        LOGGER.info("Purchasing tickets " + tickets.toString());
        InvoiceCreateDto invoiceToCreate = new InvoiceCreateDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            tickets.getFirst().getEvent().getDateTime(),
            ticketIds
        );

        LOGGER.info("Invoice to create " + invoiceToCreate);
        SimpleInvoiceDto simpleInvoice = invoiceService.create(invoiceToCreate);
        Invoice invoice = invoiceService.findById(simpleInvoice.id(), userEmail);

        Reservation reservationToDelete = null;
        for (Ticket ticket : tickets) {
            if (ticket.getReservation() != null) {
                LOGGER.info("This ticket was previously reserved " + ticket.getId());
                verifyTicketOwnership(ticket, userEmail);
                reservationToDelete = ticket.getReservation();
                ticket.setReservation(null);
            }
            ticket.setInvoice(invoice);
        }

        ticketRepository.saveAll(tickets);
        ticketRepository.flush();

        if (reservationToDelete != null) {
            reservationRepository.delete(reservationToDelete);
        }

        return ticketRepository.saveAll(tickets).stream()
            .map(ticketMapper::toDetailed)
            .toList();
    }


    @Transactional
    @Override
    public List<DetailedTicketDto> reserve(Reservation reservation, List<Long> ticketIds, String userEmail) {
        findUserOrThrow(userEmail);
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);


        for (Ticket ticket : tickets) {
            ticket.setReservation(reservation);
        }
        List<Ticket> updatedTickets = ticketRepository.saveAll(tickets);

        return updatedTickets.stream()
            .map(ticketMapper::toDetailed)
            .toList();
    }

    @Transactional
    @Override
    public void deleteByIds(List<Long> ids, String userEmail) throws AccessDeniedException {

        findUserOrThrow(userEmail);
        List<Ticket> tickets = ticketRepository.findAllById(ids);
        for (Ticket t : tickets) {
            verifyTicketOwnership(t, userEmail);
        }
        LOGGER.info("Deleting tickets " + tickets);
        ticketRepository.deleteAll(tickets);
    }

    @Override
    public List<CancelledTicket> createCancelledTickets(List<Long> ticketIds, String userEmail, Invoice creditInvoice) {
        User user = userService.findByEmail(userEmail);
        List<CancelledTicket> cancelledTickets = new ArrayList<>();
        for (Ticket t : ticketRepository.findAllById(ticketIds)) {
            CancelledTicket cancelledTicket =
                new CancelledTicket(t.getInvoice().getUser(), t.getEvent().getTitle(), t.getEvent().getDateTime(), t.getInvoice().getInvoiceDate(),
                    "" + t.getSeat().getRowNumber() + "/" + t.getSeat().getSeatNumber(), t.getNetPrice(), t.getTaxRate(), t.getGrossPrice(), creditInvoice);
            cancelledTicketRepository.save(cancelledTicket);
            cancelledTickets.add(cancelledTicket);
        }
        return cancelledTickets;
    }


}



