package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;
    private PasswordResetToken validToken;

    @BeforeEach
    void setup() {
        user = new User(
            "user@test.com",
            "OLD_HASH",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        user.setId(1L);
        user.setLocked(true);
        user.setLoginFailCount(5);

        validToken = new PasswordResetToken();
        validToken.setUser(user);
        validToken.setTokenHash("HASHED_TOKEN");
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
    }

    // -------------------------------------------------------------------------
    // triggerPasswordReset
    // -------------------------------------------------------------------------

    @Test
    void triggerPasswordReset_validUser_createsTokenAndSendsMail() {
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(anyString()))
            .thenReturn("HASHED_TOKEN");

        doNothing().when(tokenRepository).deleteByUserId(1L);
        doNothing().when(tokenRepository).flush();
        when(tokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.triggerPasswordReset(1L);

        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).flush();

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
            ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void triggerPasswordReset_unknownUser_throwsNotFoundException() {
        when(userRepository.findById(99L))
            .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
            NotFoundException.class,
            () -> passwordResetService.triggerPasswordReset(99L)
        );

        assertThat(ex.getMessage()).contains("User with id 99 not found");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(mailSender);
    }

    // -------------------------------------------------------------------------
    // resetPassword
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_validToken_resetsPasswordAndUnlocksUser() {
        String rawToken = UUID.randomUUID().toString();

        when(tokenRepository.findAll())
            .thenReturn(List.of(validToken));

        when(passwordEncoder.matches(rawToken, "HASHED_TOKEN"))
            .thenReturn(true);

        when(passwordEncoder.encode("newPassword"))
            .thenReturn("NEW_HASH");

        passwordResetService.resetPassword(rawToken, "newPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(savedUser.isLocked()).isFalse();
        assertThat(savedUser.getLoginFailCount()).isEqualTo(0);

        verify(tokenRepository).delete(validToken);
    }

    @Test
    void resetPassword_invalidToken_throwsConflictException() {
        when(tokenRepository.findAll())
            .thenReturn(List.of(validToken));

        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(false);

        ConflictException ex = assertThrows(
            ConflictException.class,
            () -> passwordResetService.resetPassword("invalid", "newPassword")
        );

        assertThat(ex.getMessage())
            .contains("Invalid or expired reset token");

        verifyNoInteractions(userRepository);
    }

    @Test
    void resetPassword_expiredToken_throwsConflictExceptionAndDeletesToken() {
        validToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findAll())
            .thenReturn(List.of(validToken));

        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(true);

        ConflictException ex = assertThrows(
            ConflictException.class,
            () -> passwordResetService.resetPassword("token", "newPassword")
        );

        assertThat(ex.getMessage()).contains("expired");

        verify(tokenRepository).delete(validToken);
        verifyNoInteractions(userRepository);
    }
}