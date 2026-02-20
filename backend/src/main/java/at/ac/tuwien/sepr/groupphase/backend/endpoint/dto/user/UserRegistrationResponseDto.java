package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

public record UserRegistrationResponseDto(
    String token,
    SimpleUserDto user
) {}