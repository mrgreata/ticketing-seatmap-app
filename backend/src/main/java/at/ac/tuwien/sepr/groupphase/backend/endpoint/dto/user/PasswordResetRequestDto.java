package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
    @NotBlank
    @Email
    String email
) {}