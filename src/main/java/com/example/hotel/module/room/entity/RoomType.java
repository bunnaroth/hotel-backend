package com.example.hotel.module.room.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 16)
	private RoomCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "image_key", nullable = false, length = 32)
	private RoomImageKey imageKey;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "max_occupancy", nullable = false)
	private short maxOccupancy;

	@Column(name = "room_limit", nullable = false)
	private int roomLimit;

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
			RoomCategory category,
			RoomImageKey imageKey,
			String description,
			short maxOccupancy,
			int roomLimit,
			BigDecimal baseNightlyRate) {
		this.code = code;
		this.name = name;
		this.category = category;
		this.imageKey = imageKey;
		this.description = description;
		this.maxOccupancy = maxOccupancy;
		this.roomLimit = roomLimit;
		this.baseNightlyRate = baseNightlyRate;
		this.active = true;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public void update(
			String code,
			String name,
			RoomCategory category,
			RoomImageKey imageKey,
			String description,
			short maxOccupancy,
			int roomLimit,
			BigDecimal baseNightlyRate) {
		this.code = code;
		this.name = name;
		this.category = category;
		this.imageKey = imageKey;
		this.description = description;
		this.maxOccupancy = maxOccupancy;
		this.roomLimit = roomLimit;
		this.baseNightlyRate = baseNightlyRate;
		this.updatedAt = Instant.now();
	}

	public void setActive(boolean active) {
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

	public RoomCategory getCategory() {
		return category;
	}

	public RoomImageKey getImageKey() {
		return imageKey;
	}

	public String getDescription() {
		return description;
	}

	public short getMaxOccupancy() {
		return maxOccupancy;
	}

	public int getRoomLimit() {
		return roomLimit;
	}

	public BigDecimal getBaseNightlyRate() {
		return baseNightlyRate;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
