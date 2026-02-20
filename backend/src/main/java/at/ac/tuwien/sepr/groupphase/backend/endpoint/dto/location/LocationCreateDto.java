package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocationCreateDto(
    @NotNull String name,
    @NotNull int zipCode,
    @NotNull @Size(max = 255) String city,
    @NotNull @Size(max = 255) String street,
    @NotNull @Size(max = 50) String streetNumber
) {

}