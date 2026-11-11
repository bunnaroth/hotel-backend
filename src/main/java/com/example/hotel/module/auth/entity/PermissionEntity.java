package com.example.hotel.module.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotel_permission")
public class PermissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "permission_id")
	private Long id;

	@Column(name = "permission_name", nullable = false, unique = true, length = 100)
	private String permissionName;

	protected PermissionEntity() {
	}

	public PermissionEntity(String permissionName) {
		this.permissionName = permissionName;
	}

	public Long getId() {
		return id;
	}

	public String getPermissionName() {
		return permissionName;
	}
}
