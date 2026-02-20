package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.reservation;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservationCreateDto(
    List<Long> tickedIds
) {

}