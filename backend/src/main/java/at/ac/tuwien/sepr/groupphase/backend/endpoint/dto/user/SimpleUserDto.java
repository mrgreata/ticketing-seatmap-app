package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

/**
 * DTO representing a simplified view of a user.
 *
 * <p>This record is used when only basic user information needs to be exposed,
 * such as the authenticated user's profile or responses from user-related endpoints.
 * It intentionally excludes sensitive fields like password hashes or internal
 * security details.
 *
 * <p>Fields:
 * - id: technical identifier of the user
 * - email: the user's unique email address
 * - userRole: the assigned application role (e.g., ROLE_USER or ROLE_ADMIN)
 */
public record SimpleUserDto(
    Long id,
    String email,
    String userRole
) {

}