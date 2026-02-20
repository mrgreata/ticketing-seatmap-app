/*package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // damit application-test.yml + H2-Test-DB verwendet wird
class EventSeatmapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSeatmap_shouldReturnAllSeatsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/events/1/seatmap"))
            .andExpect(status().isOk())
            // eventId aus SeatmapDto
            .andExpect(jsonPath("$.eventId").value(1))
            // 3 Steh-Reihen (14 + 16 + 18 = 48)
            // + 3 Cheap-Reihen à 20 (= 60)
            // + 5 Standard-Reihen à 20 (= 100)
            // + Premium: 3x20 + 18 + 16 + 14 (= 108)
            // = 48 + 60 + 100 + 108 = 316 Seats
            .andExpect(jsonPath("$.seats", hasSize(316)));
    }
}
*/