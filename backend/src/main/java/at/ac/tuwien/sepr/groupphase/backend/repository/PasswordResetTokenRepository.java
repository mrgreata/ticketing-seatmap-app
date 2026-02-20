package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for accessing and managing {@link PasswordResetToken} entities.
 *
 * <p>This repository provides CRUD operations for password reset tokens and
 * defines additional query methods required for secure password reset workflows.
 *
 * <p>Each reset token is associated with exactly one user and is
 * intended for one-time use only.
 */
@Repository
public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Deletes all password reset tokens associated with the given user ID.
     *
     * <p>This method is invoked before issuing a new reset token
     * to guarantee that at most one active token exists per user.
     *
     * @param userId the ID of the user whose reset tokens should be removed
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Retrieves the password reset token associated with the given user ID.
     *
     * @param userId the ID of the user
     * @return an optional reset token for the user
     */
    Optional<PasswordResetToken> findByUserId(Long userId);
}