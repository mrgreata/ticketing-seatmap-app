package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

/**
 * Integration test verifying the complete password reset flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTriggersPasswordReset_tokenIsPersisted_andMailIsSent() throws Exception {
        tokenRepository.deleteAll();

        User user = new User(
            "reset@example.com",
            passwordEncoder.encode("secret123"),
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        user = userRepository.save(user);

        mockMvc.perform(
                post("/api/v1/admin/users/" + user.getId() + "/password-reset")
            )
            .andExpect(status().isNoContent());

        assertThat(tokenRepository.findAll()).hasSize(1);

        PasswordResetToken token = tokenRepository.findAll().getFirst();

        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailSender, atLeastOnce())
            .send(Mockito.any(SimpleMailMessage.class));
    }

    @Test
    void userRequestsPasswordReset_tokenIsPersisted_andMailIsSent() throws Exception {

        User user = new User(
            "forgot@example.com",
            passwordEncoder.encode("secret123"),
            UserRole.ROLE_USER,
            "Lisa",
            "Musterfrau",
            null
        );
        userRepository.save(user);

        mockMvc.perform(
                post("/api/v1/users/password-reset/request")
                    .contentType("application/json")
                    .content("""
                        {
                          "email": "forgot@example.com"
                        }
                        """)
            )
            .andExpect(status().isNoContent());

        assertThat(tokenRepository.findAll()).hasSize(1);

        PasswordResetToken token = tokenRepository.findAll().getFirst();

        assertThat(token.getUser().getEmail()).isEqualTo("forgot@example.com");
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailSender).send(Mockito.any(SimpleMailMessage.class));
    }

    @Test
    void confirmPasswordReset_validToken_passwordIsUpdated_andTokenIsDeleted() throws Exception {

        tokenRepository.deleteAll();

        User user = new User(
            "confirm@example.com",
            passwordEncoder.encode("oldPassword"),
            UserRole.ROLE_USER,
            "Anna",
            "Example",
            null
        );
        user.setLocked(true);
        user.setLoginFailCount(5);
        user = userRepository.save(user);

        String rawToken = "RAW_TOKEN";

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(passwordEncoder.encode(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        mockMvc.perform(
                post("/api/v1/users/password-reset/confirmation")
                    .contentType("application/json")
                    .content("""
                    {
                      "token": "RAW_TOKEN",
                      "newPassword": "newSecurePassword"
                    }
                    """)
            )
            .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(passwordEncoder.matches(
            "newSecurePassword",
            updatedUser.getPasswordHash()
        )).isTrue();

        assertThat(updatedUser.isLocked()).isFalse();
        assertThat(updatedUser.getLoginFailCount()).isZero();

        assertThat(tokenRepository.findAll()).isEmpty();
    }

    /**
     * Test configuration providing a mocked {@link JavaMailSender}
     * to avoid sending real e-mails during integration tests.
     */
    @TestConfiguration
    static class MailTestConfig {

        @Bean
        @Primary
        JavaMailSender mailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }
}