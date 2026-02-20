package at.ac.tuwien.sepr.groupphase.backend.endpoint.exceptionhandler;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.errors.ErrorResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.AccountLockedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.InvalidCredentialsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handling component for the entire application.
 *
 * <p>This class intercepts exceptions thrown by controllers and translates them
 * into consistent HTTP responses using {@link ErrorResponseDto}. It provides
 * dedicated handlers for domain-specific exceptions such as validation errors,
 * authentication failures, locked accounts, conflicts, and missing resources.
 * All responses follow a unified structure and include timestamps, status codes,
 * messages, and detailed error information.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to integrate with Spring's
 * built-in validation error handling.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Handles cases where a requested resource cannot be found.
     *
     * @param ex      the thrown NotFoundException
     * @param request the current web request
     * @return a {@link ResponseEntity} containing an {@link ErrorResponseDto} and HTTP 404
     */
    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        LOGGER.warn("NotFoundException: {}", ex.getMessage());

        return buildErrorResponse(
            ex,
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            List.of(ex.getMessage()),
            request
        );
    }

    /**
     * Handles custom validation errors thrown from the service layer.
     * These represent semantic validation failures rather than DTO validation issues.
     *
     * @param ex      the ValidationException containing details about the violations
     * @param request the current web request
     * @return a response with HTTP 422 Unprocessable Entity
     */
    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<Object> handleValidation(ValidationException ex, WebRequest request) {
        LOGGER.debug("ValidationException: {}", ex.getMessage());

        return buildErrorResponse(
            ex,
            HttpStatus.UNPROCESSABLE_ENTITY,
            ex.getMessage(),
            ex.getErrors(),
            request
        );
    }

    /**
     * Handles conflict situations such as duplicate data (e.g. email already registered).
     *
     * @param ex      the ConflictException thrown by the business logic
     * @param request the current web request
     * @return a response with HTTP 409 Conflict
     */
    @ExceptionHandler(ConflictException.class)
    protected ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        LOGGER.info("ConflictException: {}", ex.getMessage());

        return buildErrorResponse(
            ex,
            HttpStatus.CONFLICT,
            ex.getMessage(),
            ex.getErrors(),
            request
        );
    }

    /**
     * Handles failed authentication attempts (wrong email/password).
     *
     * @param ex      the InvalidCredentialsException thrown by authentication logic
     * @param request the current web request
     * @return a response with HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    protected ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        LOGGER.warn("InvalidCredentialsException: {}", ex.getMessage());

        return buildErrorResponse(
            ex,
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password",
            List.of(ex.getMessage()),
            request
        );
    }

    /**
     * Handles cases where a user account is locked due to repeated failed login attempts.
     *
     * @param ex      the AccountLockedException triggered by authentication logic
     * @param request the current web request
     * @return a response with HTTP 423 Locked
     */
    @ExceptionHandler(AccountLockedException.class)
    protected ResponseEntity<Object> handleAccountLocked(AccountLockedException ex, WebRequest request) {
        LOGGER.warn("AccountLockedException: {}", ex.getMessage());

        return buildErrorResponse(
            ex,
            HttpStatus.LOCKED,
            "Account is locked due to too many failed login attempts",
            List.of(ex.getMessage()),
            request
        );
    }

    /**
     * Handles validation errors originating from Spring's @Valid annotation
     * on request DTOs (syntactic or field-level validation).
     *
     * @param ex      MethodArgumentNotValidException containing DTO field errors
     * @param headers HTTP headers
     * @param status  HTTP status code determined by Spring
     * @param request the current web request
     * @return a response with HTTP 422 Unprocessable Entity
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        LOGGER.debug("Spring DTO validation error: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + " " + err.getDefaultMessage())
            .collect(Collectors.toList());

        return buildErrorResponse(
            ex,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "DTO validation failed",
            errors,
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(
            ex,
            HttpStatus.FORBIDDEN,
            ex.getMessage(),
            List.of(ex.getMessage()),
            request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(
            ex,
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            List.of(ex.getMessage()),
            request
        );
    }


    /**
     * Builds a consistent {@link ErrorResponseDto} for all handled exceptions.
     *
     * @param ex      the thrown exception
     * @param status  the HTTP status to return
     * @param message the main message shown to the client
     * @param errors  a list of detailed error messages
     * @param request the current web request
     * @return a fully configured {@link ResponseEntity} containing the error response body
     */
    private ResponseEntity<Object> buildErrorResponse(
        Exception ex,
        HttpStatus status,
        String message,
        List<String> errors,
        WebRequest request
    ) {
        ErrorResponseDto body = new ErrorResponseDto(
            Instant.now(),
            status.value(),
            message,
            errors,
            request.getDescription(false).replace("uri=", "")
        );

        return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
    }



}