package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO used for registering a new user through the public registration endpoint.
 *
 * <p>It contains all necessary information required to create a user account,
 * including authentication data (email, password), the assigned role, and
 * personal details such as first name, last name.
 *
 * <p>Validation annotations ensure that required fields are present and have
 * appropriate format or length restrictions.
 */
public record UserRegisterDto(
    @NotNull @Email String email,
    @NotNull @Size(min = 6) String password,

    @NotNull @Size(min = 1, max = 255) String firstName,
    @NotNull @Size(min = 1, max = 255) String lastName
) {
}