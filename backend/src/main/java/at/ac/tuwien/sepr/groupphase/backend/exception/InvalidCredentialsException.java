package at.ac.tuwien.sepr.groupphase.backend.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCredentialsException(Exception e) {
        super(e);
    }
}