package com.example.hotel.module.room.entity;

import java.time.Instant;

import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotel_room")
public class HotelRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "room_id")
	private Long id;

	@Column(name = "room_number", nullable = false, unique = true, length = 32)
	private String roomNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@Column(name = "floor_label", length = 32)
	private String floorLabel;

	@Enumerated(EnumType.STRING)
	@Column(name = "operational_status", nullable = false, length = 24)
	private OperationalStatus operationalStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "housekeeping_status", nullable = false, length = 24)
	private HousekeepingStatus housekeepingStatus;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected HotelRoom() {
	}

	public HotelRoom(
			String roomNumber,
			RoomType roomType,
			String floorLabel,
			OperationalStatus operationalStatus,
			HousekeepingStatus housekeepingStatus) {
		update(roomNumber, roomType, floorLabel, operationalStatus, housekeepingStatus);
	}

	public void update(
			String roomNumber,
			RoomType roomType,
			String floorLabel,
			OperationalStatus operationalStatus,
			HousekeepingStatus housekeepingStatus) {
		this.roomNumber = roomNumber.trim();
		this.roomType = roomType;
		this.floorLabel = floorLabel == null || floorLabel.isBlank() ? null : floorLabel.trim();
		this.operationalStatus = operationalStatus;
		this.housekeepingStatus = housekeepingStatus;
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getRoomNumber() {
		return roomNumber;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public String getFloorLabel() {
		return floorLabel;
	}

	public OperationalStatus getOperationalStatus() {
		return operationalStatus;
	}

	public HousekeepingStatus getHousekeepingStatus() {
		return housekeepingStatus;
	}
}
