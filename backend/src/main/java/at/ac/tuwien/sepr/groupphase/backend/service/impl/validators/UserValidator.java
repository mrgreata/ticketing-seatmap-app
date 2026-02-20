package at.ac.tuwien.sepr.groupphase.backend.service.impl.validators;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserProfileUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class UserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserRepository userRepository;

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "^(.+?)\\s+\\d+(?:\\s*(?:\\/|,)?\\s*(?:Stiege|Stg|Tuer|Tür|Top|TOP|Whg|EG|OG)\\s*[A-Za-z0-9\\-]*)*,\\s*\\d{4,5}\\s+(?=.*[A-Za-zÄÖÜäöüß])[A-Za-zÄÖÜäöüß\\s\\-]+,\\s*[A-Z]{2}$"
    );

    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Validiert fachliche Regeln für die Registrierung eines neuen Users.
     */
    public void validateForRegister(UserRegisterDto user) {
        LOGGER.debug("Validating user registration for email={}", user.email());

        List<String> conflictErrors = new ArrayList<>();

        if (userRepository.findByEmail(user.email()).isPresent()) {
            conflictErrors.add("Email already in use: " + user.email());
        }

        if (!conflictErrors.isEmpty()) {
            throw new ConflictException("Conflict during user registration", conflictErrors);
        }
    }

    /**
     * Validiert fachliche Regeln für das Anlegen eines neuen Users durch einen Administrator.
     */
    public void validateForCreate(UserCreateDto user) {
        LOGGER.debug("Validating admin user creation for email={}", user.email());
        List<String> conflictErrors = new ArrayList<>();


        if (userRepository.findByEmail(user.email()).isPresent()) {
            conflictErrors.add("Email already in use: " + user.email());
        }

        if (!conflictErrors.isEmpty()) {
            throw new ConflictException("Conflict during admin user creation", conflictErrors);
        }
    }

    /**
     * Validates user data when a user edits their profile.
     *
     * @param user before update
     * @param dto of the updated user
     */
    public void validateForUpdate(@Valid User user, UserProfileUpdateDto dto) {
        LOGGER.debug("Validating user edit for (prev) email={}", user.getEmail());

        List<String> conflictErrors = new ArrayList<>();

        if (!user.getEmail().equals(dto.email())) {
            Optional<User> existingUser = userRepository.findByEmail(dto.email());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                conflictErrors.add("Email already in use: " + user.getEmail());
            }
        }

        if (!conflictErrors.isEmpty()) {
            throw new ConflictException("Conflict during user edit", conflictErrors);
        }
    }

}