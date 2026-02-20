package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ReservationMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.CartService;
import at.ac.tuwien.sepr.groupphase.backend.service.ReservationService;

import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;


import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReservationRepository reservationRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final ReservationMapper reservationMapper;
    private final CartService cartService;

    public ReservationServiceImpl(ReservationRepository reservationRepository, @Lazy TicketService ticketService, UserService userService,
                                  ReservationMapper reservationMapper, @Lazy CartService cartService) {
        this.reservationRepository = reservationRepository;
        this.ticketService = ticketService;
        this.userService = userService;
        this.reservationMapper = reservationMapper;
        this.cartService = cartService;
    }

    private User findUserOrThrow(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found: " + email);
        }
        return user;
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }

    private void verifyReservationOwnership(Reservation reservation, String userEmail) throws AccessDeniedException {
        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied for reservation: " + reservation.getId());
        }
    }


    @Override
    public List<Reservation> findAll() {
        LOGGER.debug("Find all reservations");
        return reservationRepository.findAll();
    }

    @Override
    public SimpleReservationDto findById(Long id, String userEmail) throws AccessDeniedException {
        Reservation reservation = findReservationOrThrow(id);
        verifyReservationOwnership(reservation, userEmail);
        return reservationMapper.toSimple(reservation);
    }

    @Override
    public SimpleReservationDto create(List<Long> ticketIds, String userEmail) {
        if (ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Ticket IDs cannot be empty");
        }

        for (Long ticketId : ticketIds) {
            try {
                Ticket ticket = ticketService.findById(ticketId);
            } catch (NotFoundException e) {
                throw new NotFoundException("Ticket not found: " + ticketId);
            }
        }
        List<Ticket> tickets = ticketService.findAllByIds(ticketIds);

        for (Ticket ticket : tickets) {
            if (ticket.getReservation() != null) {
                throw new ValidationException("Ticket already exists for reservation: " + ticket.getId());
            }
            if (ticket.getInvoice() != null) {
                throw new ValidationException("Ticket " + ticket.getId() + " is already purchased (has invoice)");
            }
        }
        User user = findUserOrThrow(userEmail);
        Reservation reservationEntity = new Reservation();
        reservationEntity.setTickets(tickets);
        reservationEntity.setUser(user);
        reservationEntity.setReservationNumber("RES_" + UUID.randomUUID());
        reservationEntity.setEvent(tickets.get(0).getEvent());
        reservationRepository.save(reservationEntity);
        reservationEntity.setReservationNumber("RES-" + LocalDate.now().getYear() + "-" + reservationEntity.getId());
        Reservation reservation = reservationRepository.save(reservationEntity);
        ticketService.reserve(reservation, ticketIds, userEmail);
        return reservationMapper.toSimple(reservation);
    }

    @Transactional
    @Override
    public List<DetailedReservationDto> findByUser(String userEmail) {
        User user = findUserOrThrow(userEmail);
        List<Reservation> reservations = reservationRepository.findByUserId(user.getId());

        return reservations.stream()
            .flatMap(r -> r.getTickets().stream()
                .map(t -> reservationMapper.toTicketDetailed(t, r))
            )
            .toList();
    }


    @Transactional
    @Override
    public void cancelReservations(List<Long> ticketIds, String userEmail) throws AccessDeniedException {
        if (ticketIds.isEmpty()) {
            throw new IllegalArgumentException("Ticket IDs cannot be empty");
        }
        User user = findUserOrThrow(userEmail);
        for (Long ticketId : ticketIds) {
            try {
                Ticket ticket = ticketService.findById(ticketId);
            } catch (NotFoundException e) {
                throw new NotFoundException("Ticket not found: " + ticketId);
            }
        }
        List<Ticket> tickets = ticketService.findAllByIds(ticketIds);
        for (Ticket ticket : tickets) {
            Reservation reservation = ticket.getReservation();
            if (reservation == null) {
                throw new NotFoundException("Ticket " + ticket.getId() + " is not reserved");
            }
            verifyReservationOwnership(reservation, userEmail);
            if (ticket.getInvoice() != null) {
                throw new ValidationException("Ticket already purchased: " + ticket.getId());
            }
        }
        for (Ticket ticket : tickets) {

            if (ticket.getInvoice() == null) {
                Reservation reservation = ticket.getReservation();
                reservation.getTickets().remove(ticket);
                ticket.setReservation(null);
                try {
                    cartService.removeTicket(userEmail, ticket.getId());
                } catch (RuntimeException e) {
                    LOGGER.debug("Ignoring cart error while canceling ticket {}: {}", ticket.getId(), e.getMessage());
                }

                ticketService.deleteById(ticket.getId());
                if (reservation.getTickets().isEmpty()) {
                    reservationRepository.delete(reservation);
                }
            }
        }
    }
}