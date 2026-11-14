package com.example.hotel.module.reservation.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.reservation.enums.ReservationStatus;
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
import jakarta.persistence.Version;

@Entity
@Table(name = "reservation")
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "reservation_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "booking_reference", nullable = false, unique = true, length = 24)
	private String bookingReference;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "primary_guest_id", nullable = false)
	private Guest primaryGuest;

	@Column(name = "check_in_date", nullable = false)
	private LocalDate checkInDate;

	@Column(name = "check_out_date", nullable = false)
	private LocalDate checkOutDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	private ReservationStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected Reservation() {
	}

	public Reservation(
			String bookingReference,
			Guest primaryGuest,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Instant expiresAt) {
		this.bookingReference = bookingReference;
		this.primaryGuest = primaryGuest;
		this.checkInDate = checkInDate;
		this.checkOutDate = checkOutDate;
		this.expiresAt = expiresAt;
		this.status = ReservationStatus.PENDING;
		this.updatedAt = Instant.now();
	}

	public void expire(Instant now) {
		if (status == ReservationStatus.PENDING && !expiresAt.isAfter(now)) {
			transitionTo(ReservationStatus.CANCELLED, now);
		}
	}

	public void transitionTo(ReservationStatus target, Instant now) {
		boolean allowed = switch (status) {
			case PENDING -> target == ReservationStatus.CONFIRMED
					|| target == ReservationStatus.CANCELLED
					|| target == ReservationStatus.NO_SHOW;
			case CONFIRMED -> target == ReservationStatus.CHECKED_IN
					|| target == ReservationStatus.CANCELLED
					|| target == ReservationStatus.NO_SHOW;
			case CHECKED_IN -> target == ReservationStatus.CHECKED_OUT;
			case CHECKED_OUT -> false;
			case CANCELLED, NO_SHOW -> false;
		};
		if (!allowed) {
			throw new IllegalStateException("Reservation cannot transition from " + status + " to " + target);
		}
		status = target;
		updatedAt = now;
	}

	public boolean isPendingAndExpired(Instant now) {
		return status == ReservationStatus.PENDING && !expiresAt.isAfter(now);
	}

	public void updatePendingExpiry(Instant expiry) {
		if (status != ReservationStatus.PENDING) {
			throw new IllegalStateException("Only pending reservations can change expiry");
		}
		expiresAt = expiry;
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getBookingReference() {
		return bookingReference;
	}

	public Guest getPrimaryGuest() {
		return primaryGuest;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public LocalDate getCheckOutDate() {
		return checkOutDate;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
