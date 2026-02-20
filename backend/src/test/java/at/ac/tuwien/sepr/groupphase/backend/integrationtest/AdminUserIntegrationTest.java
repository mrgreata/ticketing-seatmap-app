package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------
    // SETUP HELPERS
    // ---------------------------------------------------------

    private void createAdminUserIfMissing() {
        userRepository.findByEmail("admin@test.com")
            .orElseGet(() -> {
                User admin = new User(
                    "admin@test.com",
                    passwordEncoder.encode("admin"),
                    UserRole.ROLE_ADMIN,
                    "Admin",
                    "User",
                    null
                );
                admin.setLocked(false);
                admin.setLoginFailCount(0);
                return userRepository.save(admin);
            });
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        UserLoginDto.UserLoginDtoBuilder
                            .anUserLoginDto()
                            .withEmail(email)
                            .withPassword(password)
                            .build()
                    ))
            )
            .andExpect(status().isOk())
            .andReturn();

        return result.getResponse().getContentAsString().replace("Bearer ", "");
    }

    private Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow()
            .getId();
    }

    // ---------------------------------------------------------
    // GET LOCKED USERS
    // ---------------------------------------------------------

    @Test
    void getLockedUsers_asAdmin_returnsLockedUsers() throws Exception {
        createAdminUserIfMissing();

        UserRegisterDto user = new UserRegisterDto(
            "locked.admin@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
            .andExpect(status().isCreated());

        UserLoginDto wrongLogin = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("locked.admin@test.com")
            .withPassword("wrongPassword")
            .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                    post("/api/v1/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin))
                )
                .andExpect(i < 4 ? status().isUnauthorized() : status().isLocked());
        }

        String adminToken = loginAndGetToken("admin@test.com", "admin");

        mockMvc.perform(
                get("/api/v1/admin/users")
                    .param("locked", "true")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + adminToken)
            )
            .andExpect(status().isOk());
    }

    // ---------------------------------------------------------
    // UNLOCK USER
    // ---------------------------------------------------------

    @Test
    void unlockUser_asAdmin_unlocksAccount() throws Exception {
        createAdminUserIfMissing();

        UserRegisterDto user = new UserRegisterDto(
            "unlock.admin@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
            .andExpect(status().isCreated());

        UserLoginDto wrongLogin = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("unlock.admin@test.com")
            .withPassword("wrongPassword")
            .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                post("/api/v1/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongLogin))
            );
        }

        Long userId = getUserIdByEmail("unlock.admin@test.com");
        String adminToken = loginAndGetToken("admin@test.com", "admin");

        mockMvc.perform(
                patch("/api/v1/admin/users/" + userId + "/lock-state")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                      "locked": false,
                      "adminLocked": false
                    }
                """)
            )
            .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------
    // BUSINESS ERROR
    // ---------------------------------------------------------

    @Test
    void unlockUser_userNotLocked_returnsConflict() throws Exception {
        createAdminUserIfMissing();

        UserRegisterDto user = new UserRegisterDto(
            "notlocked@test.com",
            "password123",
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/users/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
            .andExpect(status().isCreated());

        Long userId = getUserIdByEmail("notlocked@test.com");
        String adminToken = loginAndGetToken("admin@test.com", "admin");

        mockMvc.perform(
                patch("/api/v1/admin/users/" + userId + "/lock-state")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                      "locked": false,
                      "adminLocked": false
                    }
                """)
            )
            .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------
    // CREATE USER
    // ---------------------------------------------------------

    @Test
    void createUser_asAdmin_createsUserSuccessfully() throws Exception {
        createAdminUserIfMissing();
        String adminToken = loginAndGetToken("admin@test.com", "admin");

        UserCreateDto newUser = new UserCreateDto(
            "anna@admin",
            "password",
            UserRole.ROLE_ADMIN,
            "Anna",
            "Admin"
        );

        mockMvc.perform(
                post("/api/v1/admin/users")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/api/v1/admin/users")
                    .param("locked", "true")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + adminToken)
            )
            .andExpect(status().isOk());
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        createAdminUserIfMissing();
        String adminToken = loginAndGetToken("admin@test.com", "admin");

        UserCreateDto user = new UserCreateDto(
            "max@mustermann",
            "password",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann"
        );

        mockMvc.perform(
                post("/api/v1/admin/users")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/admin/users")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value("Conflict during admin user creation"))
            .andExpect(jsonPath("$.errors[0]")
                .value(containsString("Email already in use")));
    }

    @Test
    void adminCannotLockHimself_returnsConflict() throws Exception {
        createAdminUserIfMissing();

        Long adminId = getUserIdByEmail("admin@test.com");
        String adminToken = loginAndGetToken("admin@test.com", "admin");

        mockMvc.perform(
                patch("/api/v1/admin/users/" + adminId + "/lock-state")
                    .with(csrf())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                      "locked": false,
                      "adminLocked": false
                    }
                """)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value("Administrators cannot lock or unlock their own account"));
    }

}