package com.example.hotel.module.reservation.entity;

import java.util.UUID;

import com.example.hotel.module.guest.entity.Guest;
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

	protected ReservationGuest() {
	}

	public ReservationGuest(Reservation reservation, Guest guest) {
		this.reservation = reservation;
		this.guest = guest;
	}

	public UUID getGuestId() {
		return guest.getId();
	}

	public Guest getGuest() {
		return guest;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public UUID getReservationId() {
		return reservation.getId();
	}

	public String getGuestName() {
		return guest.getFullName();
	}

	public boolean isPrimary() {
		return reservation.getPrimaryGuest().getId().equals(guest.getId());
	}
}
