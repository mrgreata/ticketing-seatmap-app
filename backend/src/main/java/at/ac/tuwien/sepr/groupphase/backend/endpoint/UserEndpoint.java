package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.PasswordResetDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.PasswordResetRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.RewardPointsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserProfileUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.TotalCentsSpentDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for user-related operations such as retrieving the current user
 * or registering a new user.
 *
 * <p>The endpoint provides both public and secured routes:
 * - "/register": allows public user registration without authentication.
 * - "/me": returns information about the currently authenticated user and
 *   requires the role ROLE_USER.
 *
 * <p>All processing logic is delegated to the UserService.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserEndpoint {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserEndpoint.class);

    /**
     * Creates a new {@code UserEndpoint}.
     *
     * @param userService the service responsible for handling user-related business logic
     */
    public UserEndpoint(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;

    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>Requires a valid JWT and the role {@code ROLE_USER}.
     *
     * @param authentication the authentication object containing the username
     * @return a DTO containing basic user information
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/me")
    public SimpleUserDto me(Authentication authentication) {
        return userService.getCurrentUserAsSimple(authentication.getName());
    }

    /**
     * Returns the detailed profile of the currently authenticated user.
     *
     * <p>Requires a valid JWT and the role {@code ROLE_USER}.
     *
     * @param authentication the authentication object containing the username
     * @return a DTO containing detailed user information
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/me/detailed")
    public DetailedUserDto meDetailed(Authentication authentication) {
        return userService.getCurrentUserAsDetailed(authentication.getName());
    }

    /**
     * Update the current user's account.
     *
     * <p>Requires a valid JWT and the role {@code ROLE_USER}.
     *
     * @param dto the updated data for the new user
     * @param authentication the authentication object containing the username
     * @return a DTO containing detailed updated user Information
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @PutMapping("/me")
    public DetailedUserDto updateProfile(
        @Valid @RequestBody UserProfileUpdateDto dto,
        Authentication authentication
    ) {
        LOGGER.info("Updating profile for user: {}", authentication.getName());

        String currentEmail = authentication.getName();
        return userService.updateProfile(currentEmail, dto);
    }

    /**
     * Delete the current user's account.
     *
     * <p>Requires a valid JWT and the role {@code ROLE_USER}.
     *
     * @param authentication the authentication object
     */
    @Secured("ROLE_USER")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication) {
        LOGGER.info("User {} requested account deletion", authentication.getName());

        userService.deleteAccount(authentication.getName());
    }

    /**
     * Registers a new user in the system.
     *
     * <p>This endpoint is publicly accessible and does not require authentication.
     * The request is validated before the user is persisted.
     *
     * @param dto the registration data for the new user
     * @return a DTO representing the newly created user
     */
    @PermitAll
    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegistrationResponseDto register(@Valid @RequestBody UserRegisterDto dto) {
        return userService.register(dto);
    }


    @PermitAll
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(
        @Valid @RequestBody PasswordResetRequestDto dto
    ) {
        LOGGER.info("Password reset requested");
        passwordResetService.requestPasswordReset(dto.email());
    }

    /**
     * Triggers the final step of a password reset using a reset token.
     *
     * <p>This endpoint is publicly accessible and allows users to set a new
     * password after following the reset link sent via email.
     */
    @PermitAll
    @PostMapping("/password-reset/confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetDto dto) {
        passwordResetService.resetPassword(dto.token(), dto.newPassword());
    }

    /**
     * Get the reward points for the user.
     *
     * @param authentication the authentication object containing the username
     * @return a DTO containing the user's reward points
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/me/reward-points")
    public RewardPointsDto myRewardPoints(Authentication authentication) {
        return userService.getRewardPoints(authentication.getName());
    }

    /**
     * Get the total amount of money the user has spent (in cents).
     *
     * @param authentication the authentication object containing the username
     * @return a DTO containing the user's total spent amount in cents
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/me/total-cents-spent")
    public TotalCentsSpentDto myTotalCentsSpent(Authentication authentication) {
        long totalCentsSpent = userService.getTotalCentsSpent(authentication.getName());
        return new TotalCentsSpentDto(totalCentsSpent);
    }
}