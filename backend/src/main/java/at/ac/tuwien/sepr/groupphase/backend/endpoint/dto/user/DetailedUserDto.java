package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

public record DetailedUserDto(
    Long id,
    String email,
    String firstName,
    String lastName,
    boolean locked,
    String userRole,
    String address
) {
}