package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for accessing and managing {@link User} entities.
 *
 * <p>This interface provides CRUD operations through {@link JpaRepository}
 * and defines additional query methods for user-specific lookups.
 *
 * <p>The most commonly used method is {@link #findByEmail(String)}, which retrieves
 * a user based on the unique e-mail address used as login credential.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrieves a user by their unique e-mail address.
     *
     * @param email the e-mail address to search for
     * @return an {@link Optional} containing the matching user, or empty if none exists
     */
    Optional<User> findByEmail(String email);



    @Query("""
    SELECT u FROM User u
    WHERE u.locked = true OR u.adminLocked = true
        """)
        Page<User> findAllLocked(Pageable pageable);

    @Query("""
    SELECT u FROM User u
    WHERE u.locked = false AND u.adminLocked = false
        """)
        Page<User> findAllUnlocked(Pageable pageable);

    @Query("""
    SELECT u FROM User u
    WHERE (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    )
    AND (u.locked = true OR u.adminLocked = true)
        """)
        Page<User> searchAllLocked(
            @Param("search") String search,
            Pageable pageable
        );

    @Query("""
    SELECT u FROM User u
    WHERE (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    )
    AND u.locked = false
    AND u.adminLocked = false
        """)
        Page<User> searchAllUnlocked(
            @Param("search") String search,
            Pageable pageable
        );



    /**
     * Counts the number of users that are assigned the given user role.
     *
     * <p>This method is primarily used for administrative consistency checks,
     * for example to ensure that at least one administrator remains in the
     * system when attempting to change user roles.
     *
     * <p>The query is automatically derived by Spring Data JPA based on the
     * {@code userRole} field of the {@link User} entity.
     *
     * @param userRole the role to count users for
     * @return the number of users that currently have the given role
     */
    long countByUserRole(UserRole userRole);

}