package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;

/**
 * Entity representing an application user.
 *
 * <p>A {@code User} contains authentication data such as email and password hash,
 * as well as additional attributes including the assigned role, personal
 * information, and security-related fields.
 *
 * <p>There are two different account lock mechanisms:
 * <ul>
 *   <li>{@code locked}: temporary lock due to too many failed login attempts</li>
 *   <li>{@code adminLocked}: manual lock enforced by an administrator</li>
 * </ul>
 *
 * <p>Instances of this class are persisted in the {@code users} table.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Email
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole userRole;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Size(max = 255)
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Number of consecutive failed login attempts.
     */
    @NotNull
    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount = 0;

    /**
     * Indicates whether the account is temporarily locked due to failed logins.
     */
    @NotNull
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    /**
     * Indicates whether the account is manually locked by an administrator.
     * If {@code true}, password reset and login are not allowed.
     */
    @NotNull
    @Column(name = "admin_locked", nullable = false)
    private boolean adminLocked = false;

    @NotNull
    @Column(name = "reward_points", nullable = false)
    private int rewardPoints = 0;

    @NotNull
    @Column(name = "total_cents_spent", nullable = false)
    private long totalCentsSpent = 0;

    /**
     * Default constructor required by JPA.
     */
    public User() {
    }

    public User(
        String email,
        String passwordHash,
        UserRole userRole,
        String firstName,
        String lastName,
        String address
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userRole = userRole;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    // --------------------
    // Getters & Setters
    // --------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public int getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(int loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isAdminLocked() {
        return adminLocked;
    }

    public void setAdminLocked(boolean adminLocked) {
        this.adminLocked = adminLocked;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public long getTotalCentsSpent() {
        return totalCentsSpent;
    }

    public void setTotalCentsSpent(long totalCentsSpent) {
        this.totalCentsSpent = totalCentsSpent;
    }

    // --------------------
    // equals / hashCode
    // --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}