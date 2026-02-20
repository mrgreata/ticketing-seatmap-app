package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.DetailedInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.InvoiceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice.SimpleInvoiceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.DetailedTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.InvoiceTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.TicketCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.CancelledTicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.TicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.CancelledTicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.InvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.LocationService;
import at.ac.tuwien.sepr.groupphase.backend.service.ReservationService;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private UserService userService;
    @Mock private SeatService seatService;
    @Mock private EventServiceImpl eventService;
    @Mock private TicketMapper ticketMapper;
    @Mock private CancelledTicketRepository cancelledTicketRepository;
    @Mock private CancelledTicketMapper cancelledTicketMapper;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks private TicketServiceImpl ticketService;

    private User user;
    private Location location;
    private Seat seat;
    private Event event;
    private Ticket ticket;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.at");

        location = new Location();
        location.setId(10L);
        location.setCity("Wien");

        Sector sector = new Sector();
        sector.setId(5L);

        PriceCategory priceCategory = new PriceCategory();
        priceCategory.setBasePrice(50);

        seat = new Seat();
        seat.setId(20L);
        seat.setRowNumber(1);
        seat.setSeatNumber(2);
        seat.setSector(sector);
        seat.setPriceCategory(priceCategory);


        event = new Event();
        event.setId(30L);
        event.setTitle("SEPR Konzert");
        event.setDateTime(LocalDateTime.now());

        ticket = new Ticket(location, seat, event);
        ticket.setId(99L);
        ticket.setGrossPrice(60.0);
    }

    @Test
    void testCreate() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@test.at");
        when(userService.findByEmail("user@test.at")).thenReturn(user);
        when(seatService.findById(20L)).thenReturn(seat);
        when(eventService.findById(30L)).thenReturn(event);
        when(ticketRepository.saveAll(any())).thenReturn(List.of(ticket));

        when(ticketMapper.fromCreateDto(any(TicketCreateDto.class))).thenAnswer(invocation -> {
            TicketCreateDto dto = invocation.getArgument(0);
            Ticket t = new Ticket();
            t.setId(99L);
            return t;
        });
        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                "SEPR Konzert",
                seat.getRowNumber(),
                seat.getSeatNumber(),
                seat.getId(),
                event.getDateTime().toLocalDate(),
                event.getDateTime().toLocalTime().toString(),
                null,
                null,
                t.getGrossPrice(),
                location.getCity(),
                false,
                event.getId(),
                seat.getSector().getId(),
                null,
                null
            );
        });

        TicketCreateDto dto = new TicketCreateDto(30L, 20L);
        List<DetailedTicketDto> result = ticketService.create(List.of(dto), auth.getName());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(99L);
        assertThat(result.get(0).eventName()).isEqualTo("SEPR Konzert");
    }


    @Test
    void testGetMyTickets() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@test.at");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1");
        invoice.setTickets(List.of(ticket));

        InvoiceTicketDto simpleTicket = new InvoiceTicketDto(
            ticket.getId(),
            event.getTitle(),
            event.getDateTime().toString()

        );

        DetailedInvoiceDto dto = new DetailedInvoiceDto(
            1L,
            "INV-1",
            LocalDate.now(),
            user.getId(),
            List.of(simpleTicket),
            List.of()
        );

        when(invoiceService.getMyInvoices(user.getEmail())).thenReturn(List.of(dto));

        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                t.getEvent().getTitle(),
                t.getSeat().getRowNumber(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getId(),
                t.getEvent().getDateTime().toLocalDate(),
                t.getEvent().getDateTime().toLocalTime().toString(),
                t.getInvoice() != null ? t.getInvoice().getInvoiceNumber() : null,
                t.getReservation() != null ? t.getReservation().getReservationNumber() : null,
                t.getGrossPrice(),
                t.getLocation().getCity(),
                false,
                t.getEvent().getId(),
                t.getSeat().getSector().getId(),
                t.getReservation() != null ? t.getReservation().getId() : null,
                t.getInvoice() != null ? t.getInvoice().getId() : null
            );
        });

        when(ticketRepository.findAllById(anyList())).thenReturn(List.of(ticket));

        List<DetailedTicketDto> tickets = ticketService.getMyPurchasedTickets(auth.getName());

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).eventName()).isEqualTo("SEPR Konzert");
    }

    @Test
    void testFindByIdFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.of(ticket));
        Ticket result = ticketService.findById(99L);
        assertThat(result).isEqualTo(ticket);
    }


    @Test
    void testFindByIdNotFound() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ticketService.findById(1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Ticket not found");
    }

    @Test
    void testFindTicketOrThrowNotFound() {
        when(ticketRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ticketService.getTicketByIdForUser(100L, "user@test.at"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Ticket not found");
    }
    @Test
    void testVerifyTicketOwnershipAccessDenied() {
        Ticket t = new Ticket();
        t.setId(200L);
        Invoice invoice = new Invoice();
        User invoiceUser = new User();
        invoiceUser.setEmail("someone@test.at");
        invoice.setUser(invoiceUser);
        t.setInvoice(invoice);

        t.setReservation(null);
        when(ticketRepository.findById(200L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> ticketService.getTicketByIdForUser(200L, "other@test.at"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void testGetMyCancelledTickets_withData() {
        when(userService.findByEmail("user@test.at")).thenReturn(user);
        CancelledTicket ct = mock(CancelledTicket.class);
        when(cancelledTicketRepository.findByUser(user)).thenReturn(List.of(ct));
        CancelledTicketDto dto = new CancelledTicketDto(
            null,
            null,
            null,
            null,
            List.of(),
            null,
            false
        );
        when(cancelledTicketMapper.toDto(ct)).thenReturn(dto);

        List<CancelledTicketDto> result = ticketService.getMyCancelledTickets(user.getEmail());
        assertThat(result).containsExactly(dto);
    }


    @Test
    void testCreateCreditInvoice_invokesInvoiceService() throws AccessDeniedException {
        ticketService.createCreditInvoice(List.of(1L, 2L), "user@test.at");
        verify(invoiceService).createCreditInvoice(List.of(1L, 2L), "user@test.at");
    }

    @Test
    void testCreateCancelledTickets() {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setUser(user);
        ticket.setInvoice(invoice);
        when(ticketRepository.findAllById(List.of(99L))).thenReturn(List.of(ticket));

        List<CancelledTicket> cancelled = ticketService.createCancelledTickets(List.of(99L), user.getEmail(), invoice);
        assertThat(cancelled).hasSize(1);
        verify(cancelledTicketRepository).save(any(CancelledTicket.class));
    }

    @Test
    void testPurchase_ticketWithReservation_deletedReservation() throws AccessDeniedException {
        Reservation res = new Reservation();
        res.setId(123L);
        res.setUser(user);
        ticket.setReservation(res);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        when(ticketRepository.findAllById(List.of(ticket.getId()))).thenReturn(List.of(ticket));

        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SimpleInvoiceDto simpleInvoice = new SimpleInvoiceDto(1L, "INV-1", user.getId());
        when(invoiceService.create(any())).thenReturn(simpleInvoice);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1");
        when(invoiceService.findById(1L, user.getEmail())).thenReturn(invoice);

        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                t.getEvent().getTitle(),
                t.getSeat().getRowNumber(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getId(),
                t.getEvent().getDateTime().toLocalDate(),
                t.getEvent().getDateTime().toLocalTime().toString(),
                t.getInvoice() != null ? t.getInvoice().getInvoiceNumber() : null,
                null,
                t.getGrossPrice(),
                t.getLocation().getCity(),
                false,
                t.getEvent().getId(),
                t.getSeat().getSector().getId(),
                null,
                t.getInvoice() != null ? t.getInvoice().getId() : null
            );
        });

        List<DetailedTicketDto> purchased = ticketService.purchase(List.of(ticket.getId()), user.getEmail());

        assertThat(purchased).hasSize(1);
        assertThat(ticket.getReservation()).isNull();
        verify(reservationRepository).delete(res);
    }

    @Test
    void testPurchase_ticketWithoutReservation() throws AccessDeniedException {
        ticket.setReservation(null);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        when(ticketRepository.findAllById(List.of(ticket.getId()))).thenReturn(List.of(ticket));

        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SimpleInvoiceDto simpleInvoice = new SimpleInvoiceDto(1L, "INV-1", user.getId());
        when(invoiceService.create(any())).thenReturn(simpleInvoice);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1");
        when(invoiceService.findById(1L, user.getEmail())).thenReturn(invoice);

        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                t.getEvent().getTitle(),
                t.getSeat().getRowNumber(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getId(),
                t.getEvent().getDateTime().toLocalDate(),
                t.getEvent().getDateTime().toLocalTime().toString(),
                t.getInvoice() != null ? t.getInvoice().getInvoiceNumber() : null,
                null,
                t.getGrossPrice(),
                t.getLocation().getCity(),
                false,
                t.getEvent().getId(),
                t.getSeat().getSector().getId(),
                null,
                t.getInvoice() != null ? t.getInvoice().getId() : null
            );
        });

        List<DetailedTicketDto> purchased = ticketService.purchase(List.of(ticket.getId()), user.getEmail());

        assertThat(purchased).hasSize(1);
        assertThat(ticket.getReservation()).isNull();
        verify(reservationRepository, never()).delete(any());
    }





    @Test
    void testPurchase() {

        when(userService.findByEmail("user@test.at")).thenReturn(user);


        when(ticketRepository.findById(99L)).thenReturn(Optional.of(ticket));

        when(ticketRepository.findAllById(List.of(99L))).thenReturn(List.of(ticket));

        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SimpleInvoiceDto simpleInvoice = new SimpleInvoiceDto(1L, "INV-1", user.getId());
        when(invoiceService.create(any(InvoiceCreateDto.class))).thenReturn(simpleInvoice);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1");
        when(invoiceService.findById(1L, user.getEmail())).thenReturn(invoice);

        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                t.getEvent().getTitle(),
                t.getSeat().getRowNumber(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getId(),
                t.getEvent().getDateTime().toLocalDate(),
                t.getEvent().getDateTime().toLocalTime().toString(),
                t.getInvoice() != null ? t.getInvoice().getInvoiceNumber() : null,
                null,
                t.getGrossPrice(),
                t.getLocation().getCity(),
                false,
                t.getEvent().getId(),
                t.getSeat().getSector().getId(),
                null,
                t.getInvoice() != null ? t.getInvoice().getId() : null
            );
        });

        List<DetailedTicketDto> purchased = ticketService.purchase(List.of(99L), "user@test.at");


        assertThat(purchased).hasSize(1);
        assertThat(purchased.get(0).invoiceNumber()).isEqualTo("INV-1");

        verify(ticketRepository).findById(99L);
        verify(ticketRepository).findAllById(List.of(99L));
        verify(ticketRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void testReserve() {
        when(userService.findByEmail("user@test.at")).thenReturn(user);
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setReservationNumber("RES-1");

        when(ticketRepository.findAllById(List.of(99L))).thenReturn(List.of(ticket));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toDetailed(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return new DetailedTicketDto(
                t.getId(),
                t.getEvent().getTitle(),
                t.getSeat().getRowNumber(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getId(),
                t.getEvent().getDateTime().toLocalDate(),
                t.getEvent().getDateTime().toLocalTime().toString(),
                null,
                t.getReservation() != null ? t.getReservation().getReservationNumber() : null,
                t.getGrossPrice(),
                t.getLocation().getCity(),
                false,
                t.getEvent().getId(),
                t.getSeat().getSector().getId(),
                t.getReservation() != null ? t.getReservation().getId() : null,
                null
            );
        });

        List<DetailedTicketDto> reserved = ticketService.reserve(reservation, List.of(99L), "user@test.at");

        assertThat(reserved).hasSize(1);
        assertThat(reserved.get(0).reservationNumber()).isEqualTo("RES-1");
        verify(ticketRepository).saveAll(List.of(ticket));
    }


    @Test
    void testDeleteByIds() {
        when(userService.findByEmail("user@test.at")).thenReturn(user);
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        ticket.setReservation(reservation);
        when(ticketRepository.findAllById(List.of(99L))).thenReturn(List.of(ticket));
        ticketService.deleteByIds(List.of(99L), "user@test.at");
        verify(ticketRepository).deleteAll(List.of(ticket));
    }

    @Test
    void testFindAllByIds() {
        when(ticketRepository.findAllById(List.of(99L))).thenReturn(List.of(ticket));
        List<Ticket> tickets = ticketService.findAllByIds(List.of(99L));
        assertThat(tickets).containsExactly(ticket);
    }

    @Test
    void testFindAll() {
        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        List<Ticket> tickets = ticketService.findAll();
        assertThat(tickets).containsExactly(ticket);
    }
}
