package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.AccountLockedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.InvalidCredentialsException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = LoginEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class LoginEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // ---------------------------------------------------------
    // SUCCESS
    // ---------------------------------------------------------

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("password123")
            .build();

        when(userService.login(any()))
            .thenReturn("JWT_TOKEN");

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk())
            .andExpect(content().string("JWT_TOKEN"));
    }

    // ---------------------------------------------------------
    // DTO VALIDATION
    // ---------------------------------------------------------

    @Test
    void login_missingEmail_returnsUnprocessableEntity() throws Exception {
        String json = """
        {
          "password": "password123"
        }
        """;

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DTO validation failed"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_invalidEmailFormat_returnsUnprocessableEntity() throws Exception {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("invalid-email")
            .withPassword("password123")
            .build();

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0]")
                .value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void login_missingPassword_returnsUnprocessableEntity() throws Exception {
        String json = """
        {
          "email": "test@example.com"
        }
        """;

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors").isArray());
    }

    // ---------------------------------------------------------
    // BUSINESS ERRORS
    // ---------------------------------------------------------

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("wrongPassword")
            .build();

        when(userService.login(any()))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value("Invalid credentials"));
    }

    @Test
    void login_lockedAccount_returnsLocked() throws Exception {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("password123")
            .build();

        when(userService.login(any()))
            .thenThrow(new AccountLockedException("Account is locked"));

        mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isLocked())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message")
                .value("Account is locked due to too many failed login attempts"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value("Account is locked"));
    }
}