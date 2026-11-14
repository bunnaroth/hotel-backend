package com.example.hotel.module.auth.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_user")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "login_name", nullable = false, unique = true, length = 254)
	private String loginName;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "employee_user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<RoleEntity> roles = new HashSet<>();

	protected UserAccount() {
	}

	public UserAccount(String loginName, String passwordHash, Set<RoleEntity> roles) {
		this.loginName = loginName;
		this.passwordHash = passwordHash;
		this.roles.addAll(roles);
	}

	public UUID getId() {
		return id;
	}

	public String getLoginName() {
		return loginName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isActive() {
		return active;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Set<RoleEntity> getRoles() {
		return roles;
	}

	public void recordFailedLogin(int maxAttempts, Instant lockUntil) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = lockUntil;
		}
		updatedAt = Instant.now();
	}

	public void resetFailedLogins() {
		failedLoginAttempts = 0;
		lockedUntil = null;
		updatedAt = Instant.now();
	}
}
