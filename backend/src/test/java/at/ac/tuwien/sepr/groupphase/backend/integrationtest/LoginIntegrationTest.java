package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
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
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------
    // SUCCESS
    // ---------------------------------------------------------

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        UserRegisterDto registerDto = new UserRegisterDto(
            "login.integration@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerDto))
            )
            .andExpect(status().isCreated());

        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("login.integration@test.com")
            .withPassword("password123")
            .build();

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDto))
            )
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.isEmptyString())));
    }

    // ---------------------------------------------------------
    // INVALID CREDENTIALS
    // ---------------------------------------------------------

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        UserRegisterDto registerDto = new UserRegisterDto(
            "wrongpw@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerDto))
            )
            .andExpect(status().isCreated());

        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("wrongpw@test.com")
            .withPassword("wrongPassword")
            .build();

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDto))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ---------------------------------------------------------
    // ACCOUNT LOCK
    // ---------------------------------------------------------

    @Test
    void login_fiveFailedAttempts_locksAccount() throws Exception {
        UserRegisterDto registerDto = new UserRegisterDto(
            "locked@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerDto))
            )
            .andExpect(status().isCreated());

        UserLoginDto wrongLogin = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("locked@test.com")
            .withPassword("wrongPassword")
            .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                    post("/api/v1/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin))
                )
                .andExpect(i < 4
                    ? status().isUnauthorized()
                    : status().isLocked());
        }

        UserLoginDto correctLogin = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("locked@test.com")
            .withPassword("password123")
            .build();

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(correctLogin))
            )
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.message")
                .value("Account is locked due to too many failed login attempts"));
    }

    // ---------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------

    @Test
    void login_missingPassword_returnsUnprocessableEntity() throws Exception {
        String json = """
        {
          "email": "missingpw@test.com"
        }
        """;

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void login_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ not valid json }")
            )
            .andExpect(status().isBadRequest());
    }
}