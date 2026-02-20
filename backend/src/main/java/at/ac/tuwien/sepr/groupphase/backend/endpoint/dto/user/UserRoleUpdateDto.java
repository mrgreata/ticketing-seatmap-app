package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateDto(

    @NotNull
    UserRole role

) {
}