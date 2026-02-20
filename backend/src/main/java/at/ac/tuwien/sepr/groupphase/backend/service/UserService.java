package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLockUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserProfileUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.RewardPointsDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;

import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface defining user-related operations such as registration,
 * authentication, account management and retrieval of user information.
 *
 * <p>Implementations of this service handle validation, persistence and
 * conversion between entities and DTOs. It acts as an abstraction layer
 * between controllers and data access.
 */
public interface UserService {

    // ------------------------------------------------------------
    // Internal lookup
    // ------------------------------------------------------------

    User findByEmail(String email);

    User findById(Long id);

    // ------------------------------------------------------------
    // Authentication / Registration
    // ------------------------------------------------------------

    UserRegistrationResponseDto register(UserRegisterDto dto);

    String login(UserLoginDto dto);

    // ------------------------------------------------------------
    // Current user / profile
    // ------------------------------------------------------------

    SimpleUserDto getCurrentUserAsSimple(String email);

    DetailedUserDto getCurrentUserAsDetailed(String email);

    DetailedUserDto updateProfile(String currentMail, UserProfileUpdateDto dto);

    void deleteAccount(String email);

    RewardPointsDto getRewardPoints(String email);

    long getTotalCentsSpent(String email);

    // ------------------------------------------------------------
    // Admin: user retrieval
    // ------------------------------------------------------------

    /**
     * Returns a paginated list of users filtered by lock state and optional search term.
     *
     * <p>This method is intended for administrative user management views
     * that need to scale to large user counts.
     *
     * @param locked whether to return locked or active users
     * @param search optional search term (first name, last name, email)
     * @param pageable pagination information
     * @return a page of detailed user DTOs
     */
    Page<DetailedUserDto> findUsers(
        boolean locked,
        String search,
        Pageable pageable
    );

    // ------------------------------------------------------------
    // Admin: user management
    // ------------------------------------------------------------

    void updateLockState(Long targetUserId, UserLockUpdateDto dto);

    void createUser(UserCreateDto dto);

    void updateUserRole(Long targetUserId, UserRole newRole);
}