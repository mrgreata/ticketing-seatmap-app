package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.DetailedReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation.SimpleReservationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ReservationMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.CartService;
import at.ac.tuwien.sepr.groupphase.backend.service.TicketService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketService ticketService;
    @Mock private UserService userService;
    @Mock private ReservationMapper reservationMapper;
    @Mock private CartService cartService;

    @InjectMocks private ReservationServiceImpl reservationService;

    private User user;
    private Ticket ticket1;
    private Ticket ticket2;
    private Reservation reservation;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Test Event");
        event.setDateTime(LocalDateTime.now());

        Seat seat1 = new Seat();
        seat1.setId(100L);
        seat1.setRowNumber(1);
        seat1.setSeatNumber(1);

        Seat seat2 = new Seat();
        seat2.setId(101L);
        seat2.setRowNumber(1);
        seat2.setSeatNumber(2);

        ticket1 = new Ticket();
        ticket1.setId(1L);
        ticket1.setEvent(event);
        ticket1.setSeat(seat1);

        ticket2 = new Ticket();
        ticket2.setId(2L);
        ticket2.setEvent(event);
        ticket2.setSeat(seat2);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        reservation.setReservationNumber("RES_1");
        reservation.setTickets(new ArrayList<>(List.of(ticket1, ticket2)));
    }

    @Test
    void findById_userMatches_returnsSimpleReservationDto() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationMapper.toSimple(any(Reservation.class)))
            .thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                return new SimpleReservationDto(
                    r.getId(),
                    r.getReservationNumber(),
                    r.getUser().getId()
                );
            });

        var auth = new TestingAuthenticationToken(user.getEmail(), "pw");

        SimpleReservationDto dto = reservationService.findById(1L, auth.getName());

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isEqualTo(user.getId());
        assertThat(dto.reservationNumber()).isEqualTo("RES_1");
    }


    @Test
    void findById_userMismatch_throwsException() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@test.com");
        reservation.setUser(otherUser);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        var auth = new TestingAuthenticationToken(user.getEmail(), "pw");

        assertThatThrownBy(() -> reservationService.findById(1L, auth.getName()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void create_reservation_savesAndReturnsReservation() {
        List<Long> ticketIds = List.of(1L, 2L);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(ticketService.findAllByIds(ticketIds)).thenReturn(List.of(ticket1, ticket2));
        when(reservationMapper.toSimple(any(Reservation.class)))
            .thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                return new SimpleReservationDto(r.getId(), r.getReservationNumber(), r.getUser().getId());
            });
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(42L);
            }
            return r;
        });

        when(ticketService.reserve(any(), eq(ticketIds), eq(user.getEmail()))).thenReturn(
            List.of()
        );
        SimpleReservationDto result = reservationService.create(ticketIds, user.getEmail());



        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.reservationNumber()).startsWith("RES-");


        verify(ticketService).reserve(any(), eq(ticketIds), eq(user.getEmail()));
        verify(reservationRepository, times(2)).save(any(Reservation.class));
    }

    @Test
    void findByUserId_returnsDetailedDtos() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(reservationRepository.findByUserId(user.getId()))
            .thenReturn(List.of(reservation));
        when(reservationMapper.toTicketDetailed(ticket1, reservation))
            .thenReturn(new DetailedReservationDto(
                reservation.getId(),
                ticket1.getId(),
                ticket1.getEvent().getTitle(),
                ticket1.getSeat().getRowNumber(),
                ticket1.getSeat().getSeatNumber(),
                ticket1.getSeat().getId(),
                ticket1.getEvent().getDateTime().toLocalDate(),
                "10:00",
                reservation.getReservationNumber(),
                100.0,
                true
            ));
        when(reservationMapper.toTicketDetailed(ticket2, reservation))
            .thenReturn(new DetailedReservationDto(
                reservation.getId(),
                ticket2.getId(),
                ticket2.getEvent().getTitle(),
                ticket2.getSeat().getRowNumber(),
                ticket2.getSeat().getSeatNumber(),
                ticket2.getSeat().getId(),
                ticket2.getEvent().getDateTime().toLocalDate(),
                "10:00",
                reservation.getReservationNumber(),
                100.0,
                true
            ));
        List<DetailedReservationDto> dtos = reservationService.findByUser(user.getEmail());

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).ticketId()).isEqualTo(ticket1.getId());
        assertThat(dtos.get(1).ticketId()).isEqualTo(ticket2.getId());
        assertThat(dtos.get(0).reservationNumber()).isEqualTo("RES_1");
    }

    @Test
    void cancelReservations_success_deletesReservationIfAllTicketsNull() {
        ticket1.setReservation(reservation);
        ticket2.setReservation(reservation);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(ticketService.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(ticket1, ticket2));

        reservationService.cancelReservations(List.of(1L, 2L), user.getEmail());

        assertThat(ticket1.getReservation()).isNull();
        assertThat(ticket2.getReservation()).isNull();
        verify(ticketService).deleteById(1L);
        verify(ticketService).deleteById(2L);
        verify(cartService).removeTicket(user.getEmail(), 1L);
        verify(cartService).removeTicket(user.getEmail(), 2L);
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void cancelReservations_ticketNotOwnedByUser_throwsException() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@test.com");
        reservation.setUser(otherUser);
        ticket1.setReservation(reservation);

        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(ticketService.findAllByIds(List.of(1L))).thenReturn(List.of(ticket1));

        assertThatThrownBy(() -> reservationService.cancelReservations(List.of(1L), user.getEmail()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }
}
