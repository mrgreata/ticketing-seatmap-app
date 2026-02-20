package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateDto(
    @NotNull
    @Email
    @Size(max = 255)
    String email,

    @NotNull
    @Size(min = 1, max = 255)
    String firstName,

    @NotNull
    @Size(min = 1, max = 255)
    String lastName,

    @Size(max = 255)
    @Pattern(
        regexp = "^.+\\s\\d{1,3}(?:/\\d{1,3}){0,2},\\s\\d{4}\\s.+$",
        message = "Address must be in format: Straße Hausnummer/Stiege/Tür, PLZ Stadt"
    )
    String address
){
}
