package at.ac.tuwien.sepr.groupphase.backend.exception;

public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
    }

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountLockedException(Exception e) {
        super(e);
    }
}