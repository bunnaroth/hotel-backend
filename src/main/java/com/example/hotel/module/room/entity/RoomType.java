package com.example.hotel.module.room.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_type")
public class RoomType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "room_type_id")
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 32)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "max_occupancy", nullable = false)
	private short maxOccupancy;

	@Column(name = "base_nightly_rate", nullable = false, precision = 12, scale = 2)
	private BigDecimal baseNightlyRate;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected RoomType() {
	}

	public RoomType(
			String code,
			String name,
			String description,
			short maxOccupancy,
			BigDecimal baseNightlyRate,
			boolean active) {
		update(code, name, description, maxOccupancy, baseNightlyRate, active);
	}

	public void update(
			String code,
			String name,
			String description,
			short maxOccupancy,
			BigDecimal baseNightlyRate,
			boolean active) {
		this.code = code.trim().toUpperCase(java.util.Locale.ROOT);
		this.name = name.trim();
		this.description = description == null || description.isBlank() ? null : description.trim();
		this.maxOccupancy = maxOccupancy;
		this.baseNightlyRate = baseNightlyRate;
		this.active = active;
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public short getMaxOccupancy() {
		return maxOccupancy;
	}

	public BigDecimal getBaseNightlyRate() {
		return baseNightlyRate;
	}

	public boolean isActive() {
		return active;
	}
}
