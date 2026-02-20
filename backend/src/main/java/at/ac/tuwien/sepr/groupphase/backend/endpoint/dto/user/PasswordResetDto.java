package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

public record PasswordResetDto(
    String token,
    String newPassword
) {}