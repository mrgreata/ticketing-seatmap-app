package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

public record JwtLoginResponseDto(
    String token,
    long expiresAt
) {}