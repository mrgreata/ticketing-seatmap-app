package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity representing a one-time password reset token.
 *
 * <p>The token itself is stored in hashed form only and is associated
 * with exactly one user account. Each token has a limited validity period
 * and is invalidated after use.
 */
@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hashed representation of the reset token.
     */
    @Column(nullable = false)
    private String tokenHash;

    /**
     * Expiration timestamp of the token.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * User associated with this reset token.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // --- getters & setters ---

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}