package at.ac.tuwien.sepr.groupphase.backend.service;

/**
 * Service interface defining operations related to password reset workflows.
 *
 * <p>This service encapsulates all business logic required to securely reset
 * user passwords without exposing sensitive information to administrators
 * or other system components.
 *
 * <p>The password reset process is token-based and follows a two-step approach:
 * first, a reset is triggered and a reset token is sent via e-mail; second,
 * the user sets a new password using the provided token.
 */
public interface PasswordResetService {

    /**
     * Triggers a password reset for the user with the given ID.
     *
     * <p>This method is intended for administrative use. It generates a
     * one-time, time-limited password reset token and sends a password reset
     * e-mail to the corresponding user.
     *
     * <p>If the user does not exist, an exception is thrown.
     *
     * @param userId the ID of the user for whom the password reset should be triggered
     */
    void triggerPasswordReset(Long userId);

    /**
     * Requests a password reset for the user with the given e-mail address.
     *
     * <p>This method is intended for self-service password reset ("forgot password")
     * scenarios. If a user with the given e-mail address exists, a password reset
     * token is generated and sent via e-mail.
     *
     * <p>If no user with the given e-mail address exists, the request is silently
     * ignored to prevent information leakage.
     *
     * @param email the e-mail address of the user requesting a password reset
     */
    void requestPasswordReset(String email);

    /**
     * Resets the user's password using the provided reset token.
     *
     * <p>This method validates the given reset token, ensures that it is still
     * valid and unused, hashes the new password and updates the user's credentials.
     *
     * <p>After a successful password reset, the token is invalidated and cannot
     * be used again.
     *
     * @param token the password reset token received via e-mail
     * @param newPassword the new password chosen by the user
     */
    void resetPassword(String token, String newPassword);
}