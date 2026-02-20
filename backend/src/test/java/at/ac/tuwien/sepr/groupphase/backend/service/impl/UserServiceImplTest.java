package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLockUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.AccountLockedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ConflictException;
import at.ac.tuwien.sepr.groupphase.backend.exception.InvalidCredentialsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.validators.UserValidator;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterDto registerDto;
    private User userEntity;
    private User savedUser;
    private SimpleUserDto simpleUserDto;
    private User loginUser;

    // ---------------------------------------------------------
    // Setup / Teardown
    // ---------------------------------------------------------

    @BeforeEach
    void setup() {
        registerDto = new UserRegisterDto(
            "test@example.com",
            "password123",
            "Max",
            "Mustermann"
        );

        userEntity = new User(
            "test@example.com",
            "RAW_PASSWORD",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );

        savedUser = new User(
            "test@example.com",
            "HASHED_PASSWORD",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        savedUser.setId(1L);

        simpleUserDto = new SimpleUserDto(
            1L,
            "test@example.com",
            "ROLE_USER"
        );

        loginUser = new User(
            "test@example.com",
            "HASHED_PASSWORD",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        loginUser.setId(1L);
        loginUser.setLocked(false);
        loginUser.setLoginFailCount(0);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ---------------------------------------------------------
    // REGISTER
    // ---------------------------------------------------------

    @Test
    void register_validUser_createsUserSuccessfully() {
        doNothing().when(userValidator).validateForRegister(registerDto);
        when(userMapper.fromRegisterDto(registerDto)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED_PASSWORD");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toSimple(savedUser)).thenReturn(simpleUserDto);
        when(jwtTokenizer.getAuthToken(
            eq("test@example.com"),
            eq(List.of("ROLE_USER"))
        )).thenReturn("JWT_TOKEN");

        UserRegistrationResponseDto result = userService.register(registerDto);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("JWT_TOKEN");
        assertThat(result.user().email()).isEqualTo("test@example.com");

        verify(userValidator).validateForRegister(registerDto);
        verify(userRepository).save(any(User.class));
        verify(jwtTokenizer).getAuthToken(anyString(), anyList());
    }

    @Test
    void register_invalidUser_throwsValidationException() {
        ValidationException exception =
            new ValidationException("Validation failed", List.of("Email invalid"));

        doThrow(exception).when(userValidator).validateForRegister(registerDto);

        ValidationException thrown = assertThrows(
            ValidationException.class,
            () -> userService.register(registerDto)
        );

        assertThat(thrown.getErrors()).containsExactly("Email invalid");
        verifyNoInteractions(userRepository);
    }

    // ---------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------

    @Test
    void login_validCredentials_returnsJwtToken() {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("password123")
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(loginUser));
        when(passwordEncoder.matches("password123", "HASHED_PASSWORD"))
            .thenReturn(true);
        when(jwtTokenizer.getAuthToken(anyString(), anyList()))
            .thenReturn("JWT_TOKEN");

        String token = userService.login(dto);

        assertThat(token).isEqualTo("JWT_TOKEN");
    }

    @Test
    void login_invalidPassword_incrementsFailCount() {
        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("wrong")
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(loginUser));
        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(false);

        assertThrows(
            InvalidCredentialsException.class,
            () -> userService.login(dto)
        );

        verify(userRepository).save(argThat(user ->
            user.getLoginFailCount() == 1 && !user.isLocked()
        ));
    }

    @Test
    void login_fifthFailedAttempt_locksAccount() {
        loginUser.setLoginFailCount(4);

        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("wrong")
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(loginUser));
        when(passwordEncoder.matches(anyString(), anyString()))
            .thenReturn(false);

        assertThrows(AccountLockedException.class, () -> userService.login(dto));

        verify(userRepository).save(argThat(user ->
            user.isLocked() && user.getLoginFailCount() == 5
        ));
    }

    @Test
    void login_lockedUser_throwsAccountLockedException() {
        loginUser.setLocked(true);

        UserLoginDto dto = UserLoginDto.UserLoginDtoBuilder
            .anUserLoginDto()
            .withEmail("test@example.com")
            .withPassword("password123")
            .build();

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(loginUser));

        assertThrows(AccountLockedException.class, () -> userService.login(dto));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ---------------------------------------------------------
    // UPDATE LOCK STATE (ADMIN)
    // ---------------------------------------------------------

    @Test
    void updateLockState_unlockLockedUser_successful() {
        User admin = new User(
            "admin@example.com",
            "HASHED",
            UserRole.ROLE_ADMIN,
            "Admin",
            "User",
            null
        );
        admin.setId(1L);

        User lockedUser = new User(
            "locked@example.com",
            "HASHED",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        lockedUser.setId(42L);
        lockedUser.setLocked(true);        // Login-Fail-Lock
        lockedUser.setAdminLocked(false);  // kein Admin-Lock
        lockedUser.setLoginFailCount(5);

        mockAuthenticatedUser("admin@example.com");

        when(userRepository.findById(42L))
            .thenReturn(Optional.of(lockedUser));
        when(userRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        UserLockUpdateDto dto = new UserLockUpdateDto(false, false);

        userService.updateLockState(42L, dto);

        verify(userRepository).save(argThat(user ->
            !user.isLocked()
                && !user.isAdminLocked()
                && user.getLoginFailCount() == 0
        ));
    }

    @Test
    void updateLockState_selfLock_throwsConflictException() {
        User admin = new User(
            "admin@example.com",
            "HASHED",
            UserRole.ROLE_ADMIN,
            "Admin",
            "User",
            null
        );
        admin.setId(1L);

        mockAuthenticatedUser("admin@example.com");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(admin));

        UserLockUpdateDto dto = new UserLockUpdateDto(true, true);

        assertThrows(
            ConflictException.class,
            () -> userService.updateLockState(1L, dto)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateLockState_noStateChange_throwsConflictException() {
        User admin = new User(
            "admin@example.com",
            "HASHED",
            UserRole.ROLE_ADMIN,
            "Admin",
            "User",
            null
        );
        admin.setId(1L);

        User alreadyLocked = new User(
            "user@example.com",
            "HASHED",
            UserRole.ROLE_USER,
            "Max",
            "Mustermann",
            null
        );
        alreadyLocked.setId(2L);
        alreadyLocked.setLocked(true);
        alreadyLocked.setAdminLocked(true);

        mockAuthenticatedUser("admin@example.com");

        when(userRepository.findById(2L))
            .thenReturn(Optional.of(alreadyLocked));
        when(userRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(admin));

        UserLockUpdateDto dto = new UserLockUpdateDto(true, true);

        assertThrows(
            ConflictException.class,
            () -> userService.updateLockState(2L, dto)
        );

        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // CREATE USER (ADMIN)
    // ---------------------------------------------------------

    @Test
    void createUser_validDto_createsUserSuccessfully() {
        UserCreateDto createDto = new UserCreateDto(
            "admin-created@example.com",
            "password123",
            UserRole.ROLE_ADMIN,
            "Anna",
            "Admin"
        );

        User mappedUser = new User(
            "admin-created@example.com",
            "RAW",
            UserRole.ROLE_ADMIN,
            "Anna",
            "Admin",
            null
        );

        doNothing().when(userValidator).validateForCreate(createDto);
        when(userMapper.fromCreateDto(createDto)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(createDto);

        verify(userRepository).save(argThat(user ->
            user.getEmail().equals("admin-created@example.com")
                && user.getPasswordHash().equals("HASHED")
                && !user.isLocked()
                && user.getLoginFailCount() == 0
        ));
    }

    // ---------------------------------------------------------
    // UPDATE USER ROLE (ADMIN)
    // ---------------------------------------------------------
    @Test
    void updateUserRole_validChange_updatesRole() {
        User admin = new User(
            "admin@example.com", "HASHED",
            UserRole.ROLE_ADMIN, "Admin", "User", null
        );
        admin.setId(1L);

        User user = new User(
            "user@example.com", "HASHED",
            UserRole.ROLE_USER, "Max", "Mustermann", null
        );
        user.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserRole(2L, UserRole.ROLE_ADMIN);

        verify(userRepository).save(argThat(u ->
            u.getUserRole() == UserRole.ROLE_ADMIN
        ));
    }

    @Test
    void updateUserRole_sameRole_throwsConflictException() {
        User user = new User(
            "user@example.com", "HASHED",
            UserRole.ROLE_USER, "Max", "Mustermann", null
        );
        user.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(
            ConflictException.class,
            () -> userService.updateUserRole(2L, UserRole.ROLE_USER)
        );

        verify(userRepository, never()).save(any());
    }
}