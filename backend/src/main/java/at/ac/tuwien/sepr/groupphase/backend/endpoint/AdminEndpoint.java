package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRoleUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLockUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;

import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for administrative user operations such as retrieving
 * all locked accounts or unlocking a specific user account.
 *
 * <p>This endpoint is intended exclusively for administrators and is protected
 * by role-based access control. Only users with the role {@code ROLE_ADMIN}
 * may access the provided operations.
 *
 * <p>All business logic is delegated to the {@link UserService}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Secured("ROLE_ADMIN")
public class AdminEndpoint {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    /**
     * Creates a new {@code AdminUserEndpoint}.
     *
     * @param userService the service responsible for handling user administration logic
     */
    public AdminEndpoint(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Returns a paginated list of users filtered by lock state and optional search term.
     *
     * @param locked whether to fetch locked or active users
     * @param page   zero-based page index
     * @param size   page size
     * @param search optional search term (first name, last name or email)
     * @return a page of users
     */
    @GetMapping
    public Page<DetailedUserDto> getUsers(
        @RequestParam(name = "locked") boolean locked,
        @RequestParam(name = "page") int page,
        @RequestParam(name = "size") int size,
        @RequestParam(name = "search", required = false) String search
    ) {
        LOGGER.info(
            "Fetching users (locked={}, page={}, size={}, search={})",
            locked, page, size, search
        );

        return userService.findUsers(
            locked,
            search,
            PageRequest.of(page, size)
        );
    }

    /**
     * Updates the lock state of a user account.
     *
     * <p>An administrator cannot lock or unlock their own account.
     *
     * @param id the ID of the user to update
     * @param dto the new lock state
     */
    @PatchMapping("/{id}/lock-state")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> updateUserLockState(
        @PathVariable(name = "id") Long id,
        @Valid @RequestBody UserLockUpdateDto dto
    ) {
        LOGGER.info("Updating lock state of user with ID {} to {}", id, dto.locked());
        userService.updateLockState(id, dto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a new user account.
     *
     * @param dto the data required to create the new user
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@Valid @RequestBody UserCreateDto dto) {
        userService.createUser(dto);
    }

    /**
     * Triggers a password reset for the given user.
     *
     * <p>This operation does NOT set a new password. Instead, it generates
     * a one-time reset token and sends a password reset email to the user.
     *
     * <p>The administrator never gains access to the user's password.
     *
     * @param id the ID of the user for whom the password reset should be triggered
     */
    @PostMapping("/{id}/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void triggerPasswordReset(@PathVariable("id") Long id) {
        passwordResetService.triggerPasswordReset(id);
    }

    @PutMapping("{id}/role")
    public void updateUserRole(
        @PathVariable(name = "id") Long id,
        @Valid @RequestBody UserRoleUpdateDto dto
    ) {
        userService.updateUserRole(id, dto.role());
    }
}