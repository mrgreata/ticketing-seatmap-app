/*package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.seat.SeatStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventSeatmapStatusIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private SeatRepository seatRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        // Reihenfolge wegen FK
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seatStatus_isSold_whenTicketExists() {
        SeatmapDto seatmap = restTemplate.getForObject("/api/events/1/seatmap", SeatmapDto.class);
        assertThat(seatmap).isNotNull();
        assertThat(seatmap.getSeats()).isNotEmpty();

        Long seatId = seatmap.getSeats().get(0).getId();

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        Event event = eventRepository.findById(1L).orElseThrow();

        ticketRepository.saveAndFlush(new Ticket(seat, event));

        SeatmapDto after = restTemplate.getForObject("/api/events/1/seatmap", SeatmapDto.class);
        assertThat(after).isNotNull();

        var seatDto = after.getSeats().stream()
            .filter(s -> s.getId().equals(seatId))
            .findFirst()
            .orElseThrow();

        assertThat(seatDto.getStatus()).isEqualTo(SeatStatus.SOLD);
    }

    @Test
    void seatStatus_isReserved_whenInReservation() {
        SeatmapDto seatmap = restTemplate.getForObject("/api/events/1/seatmap", SeatmapDto.class);
        assertThat(seatmap).isNotNull();
        assertThat(seatmap.getSeats().size()).isGreaterThan(1);

        Long seatId = seatmap.getSeats().get(1).getId();

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        Event event = eventRepository.findById(1L).orElseThrow();

        // garantiert 60 Zeichen (Constraint @Size(60,60))
        String hash60 = "a".repeat(60);

        User user = new User("test@test.com", hash60, UserRole.ROLE_USER);
        user = userRepository.saveAndFlush(user);

        Reservation reservation = new Reservation();
        reservation.setReservationNumber("RES-1");
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeats(List.of(seat));

        reservationRepository.saveAndFlush(reservation);

        SeatmapDto after = restTemplate.getForObject("/api/events/1/seatmap", SeatmapDto.class);
        assertThat(after).isNotNull();

        var seatDto = after.getSeats().stream()
            .filter(s -> s.getId().equals(seatId))
            .findFirst()
            .orElseThrow();

        assertThat(seatDto.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
}
*/