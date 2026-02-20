package at.ac.tuwien.sepr.groupphase.backend.exception;

import java.util.List;

/**
 * Exception indicating that the input provided by the client fails
 * validation rules defined by the application.
 *
 * <p>This exception is typically thrown when user-supplied data violates
 * format constraints, required fields are missing, or business validation
 * rules are not fulfilled.
 *
 * <p>It may include a list of specific validation error messages to provide
 * precise feedback to the caller.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    /**
     * Creates a new {@code ValidationException} with a message but without
     * additional error details.
     *
     * @param message description of the validation failure
     */
    public ValidationException(String message) {
        super(message);
        this.errors = List.of();
    }

    /**
     * Creates a new {@code ValidationException} with a message and a list of
     * specific validation errors.
     *
     * @param message description of the validation failure
     * @param errors  list of detailed validation violations
     */
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * Returns the list of detailed validation errors associated with this exception.
     *
     * @return immutable list of validation error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}