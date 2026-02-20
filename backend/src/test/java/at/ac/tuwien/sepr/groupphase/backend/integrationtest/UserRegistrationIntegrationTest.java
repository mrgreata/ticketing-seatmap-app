package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_validUser_createsUserAndReturnsToken() throws Exception {
        UserRegisterDto dto = new UserRegisterDto(
            "integration@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.id").isNumber())
            .andExpect(jsonPath("$.user.email").value("integration@test.com"))
            .andExpect(jsonPath("$.user.userRole").value("ROLE_USER"));
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        UserRegisterDto dto = new UserRegisterDto(
            "duplicate@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(post("/api/v1/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_invalidPassword_returnsBadRequest() throws Exception {
        UserRegisterDto dto = new UserRegisterDto(
            "invalid@test.com",
            "123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(post("/api/v1/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is not valid json }"))
            .andExpect(status().isBadRequest());
    }
}