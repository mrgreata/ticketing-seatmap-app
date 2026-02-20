package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArtistCreateDto(
    @NotNull @Size(max = 255) String name,
    Boolean isBand,
    List<Long> memberIds
) {

}