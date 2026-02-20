package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.errors;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response returned by all REST exception handlers.
 */
public record ErrorResponseDto(
    Instant timestamp,
    int status,
    String message,
    List<String> errors,
    String path
) {}