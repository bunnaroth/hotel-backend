package com.example.hotel.module.reservation.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ReservationGuestId implements Serializable {

	private UUID reservation;
	private UUID guest;

	public ReservationGuestId() {
	}

	public ReservationGuestId(UUID reservation, UUID guest) {
		this.reservation = reservation;
		this.guest = guest;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ReservationGuestId that)) {
			return false;
		}
		return Objects.equals(reservation, that.reservation) && Objects.equals(guest, that.guest);
	}

	@Override
	public int hashCode() {
		return Objects.hash(reservation, guest);
	}
}
