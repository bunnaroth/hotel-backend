package com.example.hotel.module.room.entity;

import java.time.Instant;

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

	@Enumerated(EnumType.STRING)
	@Column(name = "operational_status", nullable = false, length = 24)
	private OperationalStatus operationalStatus;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected HotelRoom() {
	}

	public HotelRoom(String roomNumber, RoomType roomType, OperationalStatus operationalStatus) {
		this.roomNumber = roomNumber;
		this.roomType = roomType;
		this.operationalStatus = operationalStatus;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public void update(String roomNumber, RoomType roomType, OperationalStatus operationalStatus) {
		this.roomNumber = roomNumber;
		this.roomType = roomType;
		this.operationalStatus = operationalStatus;
		this.updatedAt = Instant.now();
	}

	public void setOperationalStatus(OperationalStatus operationalStatus) {
		this.operationalStatus = operationalStatus;
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

	public OperationalStatus getOperationalStatus() {
		return operationalStatus;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
