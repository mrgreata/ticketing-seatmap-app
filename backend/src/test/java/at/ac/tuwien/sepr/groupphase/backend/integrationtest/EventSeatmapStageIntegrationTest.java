/*package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.event.SeatmapDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventSeatmapStageIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getSeatmap_event1_containsStageFromLocation() {
        ResponseEntity<SeatmapDto> resp =
            restTemplate.getForEntity("/api/events/1/seatmap", SeatmapDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        SeatmapDto dto = resp.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getEventId()).isEqualTo(1L);
        assertThat(dto.getStagePosition()).isEqualTo("BOTTOM");
        assertThat(dto.getStageLabel()).isEqualTo("BÜHNE");
        assertThat(dto.getSeats()).isNotEmpty();
    }


    @Test
    void getSeatmap_eachEventHasDifferentStage() {
        SeatmapDto s2 = restTemplate.getForObject("/api/events/2/seatmap", SeatmapDto.class);
        SeatmapDto s3 = restTemplate.getForObject("/api/events/3/seatmap", SeatmapDto.class);
        SeatmapDto s4 = restTemplate.getForObject("/api/events/4/seatmap", SeatmapDto.class);

        assertThat(s2.getStagePosition()).isEqualTo("TOP");
        assertThat(s3.getStagePosition()).isEqualTo("LEFT");
        assertThat(s4.getStagePosition()).isEqualTo("RIGHT");
    }

    @Test
    void getSeatmap_unknownEvent_returns404() {
        ResponseEntity<String> resp =
            restTemplate.getForEntity("/api/events/9999/seatmap", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}*/
