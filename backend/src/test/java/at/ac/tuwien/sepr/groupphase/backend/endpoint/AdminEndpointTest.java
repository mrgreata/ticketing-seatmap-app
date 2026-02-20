package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLockUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRoleUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AdminEndpoint.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthorizationFilter.class
    )
)
@Import(AdminEndpointTest.TestSecurityConfig.class)
@AutoConfigureMockMvc
class AdminEndpointTest {

    @TestConfiguration
    @EnableMethodSecurity(securedEnabled = true)
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    // ---------------------------------------------------------
    // GET USERS (PAGINATED)
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_lockedUsers_returnsPage() throws Exception {
        DetailedUserDto dto = new DetailedUserDto(
            1L,
            "locked@example.com",
            "Max",
            "Mustermann",
            true,
            "ROLE_USER",
            "Karlsplatz 13, 1040 Wien"
        );

        Page<DetailedUserDto> page =
            new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

        when(userService.findUsers(eq(true), any(), any()))
            .thenReturn(page);

        mockMvc.perform(
                get("/api/v1/admin/users")
                    .param("locked", "true")
                    .param("page", "0")
                    .param("size", "5")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].email").value("locked@example.com"))
            .andExpect(jsonPath("$.content[0].locked").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUsers_asNonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(
                get("/api/v1/admin/users")
                    .param("locked", "true")
                    .param("page", "0")
                    .param("size", "5")
            )
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // UPDATE LOCK STATE
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateLockState_unlockUser_returnsNoContent() throws Exception {

        UserLockUpdateDto dto = new UserLockUpdateDto(false, false);

        doNothing().when(userService)
            .updateLockState(eq(1L), any(UserLockUpdateDto.class));

        mockMvc.perform(
                patch("/api/v1/admin/users/1/lock-state")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void updateLockState_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(
                patch("/api/v1/admin/users/1/lock-state")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"locked\":false,\"adminLocked\":false}")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateLockState_userNotFound_returnsNotFound() throws Exception {

        doThrow(new NotFoundException("User with id 99 not found"))
            .when(userService)
            .updateLockState(eq(99L), any(UserLockUpdateDto.class));

        mockMvc.perform(
                patch("/api/v1/admin/users/99/lock-state")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"locked\":false,\"adminLocked\":false}")
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("User with id 99 not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateLockState_noStateChange_returnsConflict() throws Exception {

        doThrow(new ConflictException("User already has the requested lock state"))
            .when(userService)
            .updateLockState(eq(2L), any(UserLockUpdateDto.class));

        mockMvc.perform(
                patch("/api/v1/admin/users/2/lock-state")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"locked\":false,\"adminLocked\":false}")
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("User already has the requested lock state"));
    }

    // ---------------------------------------------------------
    // CREATE USER
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_returnsCreated() throws Exception {
        UserCreateDto dto = new UserCreateDto(
            "newuser@example.com",
            "password123",
            UserRole.ROLE_USER,
            "Anna",
            "Admin"
        );

        mockMvc.perform(
                post("/api/v1/admin/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------
    // PASSWORD RESET
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void triggerPasswordReset_asAdmin_returnsNoContent() throws Exception {

        mockMvc.perform(
                post("/api/v1/admin/users/1/password-reset")
                    .with(csrf())
            )
            .andExpect(status().isNoContent());

        verify(passwordResetService)
            .triggerPasswordReset(1L);
    }

    // ---------------------------------------------------------
    // UPDATE USER ROLE
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_asAdmin_returnsOk() throws Exception {
        UserRoleUpdateDto dto = new UserRoleUpdateDto(UserRole.ROLE_ADMIN);

        mockMvc.perform(
                put("/api/v1/admin/users/2/role")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk());

        verify(userService).updateUserRole(2L, UserRole.ROLE_ADMIN);
    }
}