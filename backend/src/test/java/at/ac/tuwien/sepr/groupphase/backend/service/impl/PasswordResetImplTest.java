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
class PasswordResetImplTest {

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

    @BeforeEach
    void setup() {
        user = new User(
            "test@example.com",
            "HASHED_PASSWORD",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        user.setId(1L);
        user.setLocked(false);
        user.setLoginFailCount(2);
    }

    @Test
    void triggerPasswordReset_validUser_createsTokenAndSendsMail() {
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(anyString()))
            .thenReturn("HASHED_TOKEN");

        passwordResetService.triggerPasswordReset(1L);

        verify(tokenRepository).deleteByUserId(user.getId());

        ArgumentCaptor<PasswordResetToken> captor =
            ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(tokenRepository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).isEqualTo("HASHED_TOKEN");

        assertThat(savedToken.getExpiresAt())
            .isAfter(LocalDateTime.now())
            .isBefore(LocalDateTime.now().plusMinutes(31));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void triggerPasswordReset_unknownUser_throwsNotFoundException() {
        when(userRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> passwordResetService.triggerPasswordReset(99L)
        );

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(mailSender);
    }

    @Test
    void resetPassword_validToken_resetsPasswordAndUnlocksUser() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("HASHED_TOKEN");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findAll())
            .thenReturn(List.of(token));

        when(passwordEncoder.matches("RAW_TOKEN", "HASHED_TOKEN"))
            .thenReturn(true);

        when(passwordEncoder.encode("newPassword"))
            .thenReturn("NEW_HASH");

        passwordResetService.resetPassword("RAW_TOKEN", "newPassword");

        verify(userRepository).save(argThat(saved ->
            saved.getPasswordHash().equals("NEW_HASH")
                && !saved.isLocked()
                && saved.getLoginFailCount() == 0
        ));

        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPassword_expiredToken_throwsConflictException() {
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUser(user);
        expiredToken.setTokenHash("HASHED_TOKEN");
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findAll())
            .thenReturn(List.of(expiredToken));

        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(true);

        assertThrows(
            ConflictException.class,
            () -> passwordResetService.resetPassword("RAW_TOKEN", "newPassword")
        );

        verify(tokenRepository).delete(expiredToken);
        verifyNoInteractions(userRepository);
    }

    @Test
    void resetPassword_invalidToken_throwsConflictException() {
        when(tokenRepository.findAll())
            .thenReturn(List.of());

        assertThrows(
            ConflictException.class,
            () -> passwordResetService.resetPassword("INVALID", "newPassword")
        );

        verifyNoInteractions(userRepository);
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void requestPasswordReset_existingEmail_createsTokenAndSendsMail() {
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(anyString()))
            .thenReturn("HASHED_TOKEN");

        passwordResetService.requestPasswordReset("test@example.com");

        verify(tokenRepository).deleteByUserId(user.getId());

        ArgumentCaptor<PasswordResetToken> captor =
            ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(tokenRepository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).isEqualTo("HASHED_TOKEN");
        assertThat(savedToken.getExpiresAt())
            .isAfter(LocalDateTime.now())
            .isBefore(LocalDateTime.now().plusMinutes(31));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNothing() {
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("unknown@example.com");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(mailSender);
    }
}