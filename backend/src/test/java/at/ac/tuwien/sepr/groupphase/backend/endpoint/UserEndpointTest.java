package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.PasswordResetDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.PasswordResetRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = UserEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class UserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void register_validUser_createsUserAndReturnsToken() throws Exception {
        UserRegisterDto dto = new UserRegisterDto(
            "test@example.com",
            "password123",
            "Max",
            "Mustermann"
        );

        SimpleUserDto simpleUser = new SimpleUserDto(
            1L,
            "test@example.com",
            "ROLE_USER"
        );

        UserRegistrationResponseDto response =
            new UserRegistrationResponseDto("JWT_TOKEN", simpleUser);

        when(userService.register(dto)).thenReturn(response);

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("JWT_TOKEN"))
            .andExpect(jsonPath("$.user.id").value(1))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.userRole").value("ROLE_USER"));
    }

    @Test
    void register_invalidPassword_returnsUnprocessableEntity() throws Exception {
        UserRegisterDto dto = new UserRegisterDto(
            "invalid@test.com",
            "123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DTO validation failed"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("password")));
    }

    @Test
    void register_missingEmail_returnsUnprocessableEntity() throws Exception {
        String json = """
        {
          "password": "password123",
          "userRole": "ROLE_USER",
          "firstName": "Max",
          "lastName": "Mustermann"
        }
        """;

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_existingEmail_returnsConflict() throws Exception {
        when(userService.register(any()))
            .thenThrow(new ConflictException("Email already exists"));

        UserRegisterDto dto = new UserRegisterDto(
            "existing@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void requestPasswordReset_existingEmail_returnsNoContent() throws Exception {
        PasswordResetRequestDto dto =
            new PasswordResetRequestDto("test@example.com");

        doNothing().when(passwordResetService)
            .requestPasswordReset("test@example.com");

        mockMvc.perform(
                post("/api/v1/users/password-reset/request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void requestPasswordReset_unknownEmail_returnsNoContent() throws Exception {
        PasswordResetRequestDto dto =
            new PasswordResetRequestDto("unknown@example.com");

        doNothing().when(passwordResetService)
            .requestPasswordReset("unknown@example.com");

        mockMvc.perform(
                post("/api/v1/users/password-reset/request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void confirmPasswordReset_validToken_returnsNoContent() throws Exception {
        PasswordResetDto dto =
            new PasswordResetDto("TOKEN", "newPassword");

        doNothing().when(passwordResetService)
            .resetPassword("TOKEN", "newPassword");

        mockMvc.perform(
                post("/api/v1/users/password-reset/confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void confirmPasswordReset_invalidToken_returnsConflict() throws Exception {
        PasswordResetDto dto =
            new PasswordResetDto("INVALID", "newPassword");

        doThrow(new ConflictException("Invalid or expired reset token"))
            .when(passwordResetService)
            .resetPassword(eq("INVALID"), eq("newPassword"));

        mockMvc.perform(
                post("/api/v1/users/password-reset/confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("Invalid or expired reset token"));
    }
}