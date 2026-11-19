package com.example.hotel.module.reservation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_lookup_challenge")
public class ReservationLookupChallenge {

	@Id
	@Column(name = "challenge_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "reservation_id")
	private UUID reservationId;

	@Column(name = "lookup_hash", nullable = false, length = 64)
	private String lookupHash;

	@Column(name = "client_ip_hash", nullable = false, length = 64)
	private String clientIpHash;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	protected ReservationLookupChallenge() {
	}

	public ReservationLookupChallenge(
			UUID id,
			UUID reservationId,
			String lookupHash,
			String clientIpHash,
			String codeHash,
			Instant expiresAt) {
		this.id = id;
		this.reservationId = reservationId;
		this.lookupHash = lookupHash;
		this.clientIpHash = clientIpHash;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
	}

	public boolean isUsableAt(Instant now, int maxAttempts) {
		return consumedAt == null && expiresAt.isAfter(now) && attemptCount < maxAttempts;
	}

	public void recordFailedAttempt() {
		attemptCount++;
	}

	public void consume(Instant now) {
		consumedAt = now;
	}

	public UUID getReservationId() {
		return reservationId;
	}

	public String getCodeHash() {
		return codeHash;
	}
}
