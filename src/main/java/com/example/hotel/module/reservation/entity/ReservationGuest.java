package com.example.hotel.module.reservation.entity;

import java.util.UUID;

import com.example.hotel.module.guest.entity.Guest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_guest")
@IdClass(ReservationGuestId.class)
public class ReservationGuest {

	@Id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "guest_id", nullable = false)
	private Guest guest;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	protected ReservationGuest() {
	}

	public ReservationGuest(Reservation reservation, Guest guest, boolean primary) {
		this.reservation = reservation;
		this.guest = guest;
		this.primary = primary;
	}

	public UUID getGuestId() {
		return guest.getId();
	}

	public UUID getReservationId() {
		return reservation.getId();
	}

	public String getGuestName() {
		return guest.getFullName();
	}

	public boolean isPrimary() {
		return primary;
	}
}
