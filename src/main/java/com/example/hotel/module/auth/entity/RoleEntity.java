package com.example.hotel.module.auth.entity;

import java.util.HashSet;
import java.util.Set;

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
@Table(name = "hotel_role")
public class RoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_id")
	private Long id;

	@Column(name = "role_name", nullable = false, unique = true, length = 64)
	private String roleName;

	@ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
	private Set<UserAccount> users = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "hotel_role_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<PermissionEntity> permissions = new HashSet<>();

	protected RoleEntity() {
	}

	public RoleEntity(String roleName) {
		this.roleName = roleName;
	}

	public Long getId() {
		return id;
	}

	public String getRoleName() {
		return roleName;
	}
}
