package at.ac.tuwien.sepr.groupphase.backend.exception;

import java.util.List;

/**
 * Exception indicating that a requested operation cannot be completed due
 * to a conflict with the current state of the system.
 *
 * <p>This exception is typically used for violations such as duplicate entities,
 * constraints preventing an update, or business rules that disallow the action.
 *
 * <p>It may optionally contain a list of detailed error messages that provide
 * additional information about the conflict.
 */
public class ConflictException extends RuntimeException {

    private final List<String> errors;

    /**
     * Creates a new {@code ConflictException} with a message but without
     * additional error details.
     *
     * @param message description of the conflict
     */
    public ConflictException(String message) {
        super(message);
        this.errors = List.of();
    }

    /**
     * Creates a new {@code ConflictException} with a message and a list of
     * specific error details.
     *
     * @param message description of the conflict
     * @param errors  list of detailed conflict violations
     */
    public ConflictException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public ConflictException(String message, List<String> errors, Throwable cause) {
        super(message, cause);
        this.errors = errors;
    }

    /**
     * Returns the list of detailed errors associated with this conflict.
     *
     * @return immutable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}