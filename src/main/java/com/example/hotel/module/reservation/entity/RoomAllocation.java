package com.example.hotel.module.reservation.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.example.hotel.module.room.entity.HotelRoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_allocation")
public class RoomAllocation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "allocation_id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private HotelRoom room;

	@Column(name = "check_in_date", nullable = false)
	private LocalDate checkInDate;

	@Column(name = "check_out_date", nullable = false)
	private LocalDate checkOutDate;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected RoomAllocation() {
	}

	public RoomAllocation(Reservation reservation, HotelRoom room) {
		this.reservation = reservation;
		this.room = room;
		this.checkInDate = reservation.getCheckInDate();
		this.checkOutDate = reservation.getCheckOutDate();
	}

	public void release() {
		active = false;
	}

	public HotelRoom getRoom() {
		return room;
	}

	public Long getRoomId() {
		return room.getId();
	}

	public UUID getReservationId() {
		return reservation.getId();
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public LocalDate getCheckOutDate() {
		return checkOutDate;
	}

	public boolean isActive() {
		return active;
	}
}
