package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateDto(

    @NotBlank
    @Email
    String email,

    @NotBlank
    String password,

    @NotNull
    UserRole userRole,

    @NotBlank
    String firstName,

    @NotBlank
    String lastName
) {
}