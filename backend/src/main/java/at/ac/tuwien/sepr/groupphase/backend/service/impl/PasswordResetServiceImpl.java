package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass());

    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public PasswordResetServiceImpl(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        PasswordEncoder passwordEncoder,
        JavaMailSender mailSender
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    // -------------------------------------------------------------------------
    // PASSWORD RESET REQUEST
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public void triggerPasswordReset(Long userId) {
        LOGGER.info("Triggering password reset for userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() ->
                new NotFoundException("User with id " + userId + " not found"));

        createTokenAndSendMail(user);

        LOGGER.info("Password reset process completed for userId={}", userId);
    }

    @Transactional
    @Override
    public void requestPasswordReset(String email) {
        LOGGER.info("Password reset requested for email={}", email);

        userRepository.findByEmail(email)
            .ifPresent(user -> {

                if (user.isAdminLocked()) {
                    LOGGER.warn(
                        "Password reset denied for admin-locked user id={} email={}",
                        user.getId(),
                        email
                    );
                    return;
                }

                createTokenAndSendMail(user);
            });
    }

    @Transactional
    @Override
    public void resetPassword(String rawToken, String newPassword) {
        LOGGER.info("Attempting password reset using token");

        PasswordResetToken resetToken = tokenRepository.findAll().stream()
            .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
            .findFirst()
            .orElseThrow(() ->
                new ConflictException("Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            LOGGER.warn("Expired password reset token used");
            tokenRepository.delete(resetToken);
            throw new ConflictException("Reset token has expired");
        }

        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLoginFailCount(0);
        user.setLocked(false);

        userRepository.save(user);
        tokenRepository.delete(resetToken);

        LOGGER.info("Password successfully reset for userId={}", user.getId());
    }

    private void createTokenAndSendMail(User user) {
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashedToken);
        token.setExpiresAt(
            LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));

        tokenRepository.save(token);

        try {
            sendResetMail(user.getEmail(), rawToken);
        } catch (MailException ex) {
            LOGGER.warn(
                "Password reset mail could not be sent to email={}",
                user.getEmail(),
                ex
            );
        }
    }

    private void sendResetMail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password reset");
        message.setText(
            "Sie haben ein Zurücksetzen Ihres Passworts angefordert.\n\n"
                + "Bitte verwenden Sie den folgenden Link, um ein neues Passwort zu setzen:\n\n"
                + "http://localhost:4200/#/password-reset/confirm?token=" + token + "\n\n"
                + "Dieser Link ist " + TOKEN_VALIDITY_MINUTES + " Minuten gültig."
        );

        mailSender.send(message);
    }
}