package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.RewardPointsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLockUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserProfileUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.AccountLockedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.InvalidCredentialsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.validators.UserValidator;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;
    private final UserValidator validator;
    private final UserMapper mapper;

    public UserServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenizer jwtTokenizer,
        UserValidator validator,
        UserMapper mapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenizer = jwtTokenizer;
        this.validator = validator;
        this.mapper = mapper;
    }

    // ============================================================
    // Authentication & Registration
    // ============================================================

    @Override
    public String login(UserLoginDto dto) {
        LOGGER.info("Attempting login for email={}", dto.getEmail());

        User user = findByEmail(dto.getEmail());

        if (user.isLocked() || user.isAdminLocked()) {
            LOGGER.warn("Login attempt for locked account email={}", dto.getEmail());
            throw new AccountLockedException("Account is locked");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {

            int fails = user.getLoginFailCount() + 1;
            user.setLoginFailCount(fails);

            if (fails >= 5) {
                user.setLocked(true);
                userRepository.save(user);

                LOGGER.warn("Account locked after 5 failed attempts email={}", dto.getEmail());
                throw new AccountLockedException("Account locked due to too many failed attempts");
            }

            userRepository.save(user);

            LOGGER.warn("Invalid login attempt {} for email={}", fails, dto.getEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            userRepository.save(user);
        }

        LOGGER.info("Successful login for email={}", user.getEmail());

        return jwtTokenizer.getAuthToken(
            user.getEmail(),
            List.of(user.getUserRole().name())
        );
    }

    @Override
    public UserRegistrationResponseDto register(UserRegisterDto dto) {
        LOGGER.info("Registering new user with email={}", dto.email());

        validator.validateForRegister(dto);

        User entity = mapper.fromRegisterDto(dto);

        entity.setPasswordHash(passwordEncoder.encode(dto.password()));
        entity.setUserRole(UserRole.ROLE_USER);
        entity.setLocked(false);
        entity.setLoginFailCount(0);

        User saved = userRepository.save(entity);

        String token = jwtTokenizer.getAuthToken(
            saved.getEmail(),
            List.of(saved.getUserRole().name())
        );

        return new UserRegistrationResponseDto(
            token,
            mapper.toSimple(saved)
        );
    }

    // ============================================================
    // Current User / Profile Data
    // ============================================================

    @Override
    public SimpleUserDto getCurrentUserAsSimple(String email) {
        return mapper.toSimple(findByEmail(email));
    }

    @Override
    public DetailedUserDto getCurrentUserAsDetailed(String email) {
        return mapper.toDetailed(findByEmail(email));
    }

    @Override
    @Transactional
    public DetailedUserDto updateProfile(String currentEmail, UserProfileUpdateDto dto) {
        LOGGER.info("Updating profile");
        User user = findByEmail(currentEmail);
        validator.validateForUpdate(user, dto);

        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setAddress(dto.address() != null && !dto.address().trim().isEmpty() ? dto.address() : null);

        User updated = userRepository.save(user);
        LOGGER.info("Profile updated for user id: {}", updated.getId());

        return mapper.toDetailed(updated);
    }

    @Override
    @Transactional
    public void deleteAccount(String email) {
        LOGGER.info("Deleting account for user: {}", email);
        User user = findByEmail(email);

        userRepository.delete(user);
        LOGGER.info("Account deleted for user: {}", email);
    }

    @Override
    public RewardPointsDto getRewardPoints(String email) {
        User user = findByEmail(email);
        return new RewardPointsDto(user.getRewardPoints());
    }

    @Override
    public long getTotalCentsSpent(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found: " + email));
        return user.getTotalCentsSpent();
    }

    // ============================================================
    // Administrative User Queries (Pagination & Search)
    // ============================================================

    @Override
    public Page<DetailedUserDto> findUsers(
        boolean locked,
        String search,
        Pageable pageable
    ) {
        LOGGER.info(
            "Fetching users (locked={}, search={}, page={})",
            locked,
            search,
            pageable.getPageNumber()
        );

        Page<User> result;

        if (search == null || search.isBlank()) {
            if (locked) {
                result = userRepository.findAllLocked(pageable);
            } else {
                result = userRepository.findAllUnlocked(pageable);
            }
        } else {
            if (locked) {
                result = userRepository.searchAllLocked(
                    search.toLowerCase(),
                    pageable
                );
            } else {
                result = userRepository.searchAllUnlocked(
                    search.toLowerCase(),
                    pageable
                );
            }
        }

        return result.map(mapper::toDetailed);
    }

    // ============================================================
    // Administrative User Management
    // ============================================================


    @Override
    public void updateLockState(Long targetUserId, UserLockUpdateDto dto) {

        LOGGER.info(
            "Updating lock state of user id={} to locked={}, adminLocked={}",
            targetUserId, dto.locked(), dto.adminLocked()
        );

        User targetUser = findById(targetUserId);
        User currentUser = getCurrentlyAuthenticatedUser();

        if (currentUser.getId().equals(targetUserId)) {
            LOGGER.warn(
                "Administrator attempted to change own lock state (id={})",
                targetUserId
            );
            throw new ConflictException(
                "Administrators cannot lock or unlock their own account"
            );
        }

        boolean lockUnchanged =
            targetUser.isLocked() == dto.locked()
                && targetUser.isAdminLocked() == dto.adminLocked();

        if (lockUnchanged) {
            LOGGER.warn(
                "Requested lock state already active for user id={}",
                targetUserId
            );
            throw new ConflictException(
                "User already has the requested lock state"
            );
        }

        targetUser.setAdminLocked(dto.adminLocked());

        targetUser.setLocked(dto.locked());

        if (!dto.locked()) {
            targetUser.setLoginFailCount(0);
        }

        userRepository.save(targetUser);

        LOGGER.info(
            "Successfully updated lock state of user id={} (locked={}, adminLocked={})",
            targetUserId, dto.locked(), dto.adminLocked()
        );
    }

    @Override
    public void createUser(UserCreateDto dto) {
        LOGGER.info("Creating new user with email={}", dto.email());

        validator.validateForCreate(dto);

        User user = mapper.fromCreateDto(dto);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setLocked(false);
        user.setLoginFailCount(0);

        userRepository.save(user);
    }

    @Override
    public void updateUserRole(Long targetUserId, UserRole newRole) {
        LOGGER.info(
            "Updating role of user id={} to role={}",
            targetUserId,
            newRole
        );

        User targetUser = findById(targetUserId);

        if (targetUser.getUserRole() == newRole) {
            LOGGER.warn(
                "Requested role already assigned for user id={}",
                targetUserId
            );
            throw new ConflictException("User already has the requested role");
        }

        if (targetUser.getUserRole() == UserRole.ROLE_ADMIN
            && newRole == UserRole.ROLE_USER) {

            long adminCount = userRepository.countByUserRole(UserRole.ROLE_ADMIN);

            if (adminCount <= 1) {
                LOGGER.warn(
                    "Attempt to remove last administrator (user id={})",
                    targetUserId
                );
                throw new ConflictException(
                    "The last administrator cannot be downgraded"
                );
            }
        }

        targetUser.setUserRole(newRole);
        userRepository.save(targetUser);

        LOGGER.info(
            "Successfully updated role of user id={} to role={}",
            targetUserId,
            newRole
        );
    }

    // ============================================================
    // Internal Helper Methods
    // ============================================================

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() ->
                new NotFoundException("User not found: " + email)
            );
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("User with id " + id + " not found")
            );
    }


    private User getCurrentlyAuthenticatedUser() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        String email = authentication.getName();
        return findByEmail(email);
    }
}