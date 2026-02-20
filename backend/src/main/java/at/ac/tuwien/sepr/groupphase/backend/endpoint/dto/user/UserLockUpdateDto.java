package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import jakarta.validation.constraints.NotNull;

/**
 * DTO used to update the lock state of a user.
 *
 * <p>There are two different lock mechanisms:
 * <ul>
 *   <li>{@code locked}: temporary lock due to too many failed login attempts</li>
 *   <li>{@code adminLocked}: permanent lock set explicitly by an administrator</li>
 * </ul>
 */
public record UserLockUpdateDto(

    @NotNull
    Boolean locked,

    @NotNull
    Boolean adminLocked
) {
}