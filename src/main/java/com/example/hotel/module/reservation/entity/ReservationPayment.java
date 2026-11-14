package com.example.hotel.module.reservation.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
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
@Table(name = "reservation_payment")
public class ReservationPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "payment_id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Column(name = "provider", nullable = false, length = 32)
	private String provider = "MANUAL";

	@Column(name = "provider_reference", length = 128)
	private String providerReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private ReservationPaymentStatus status;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "checkout_session_id", unique = true, length = 255)
	private String checkoutSessionId;

	@Column(name = "checkout_url", length = 2048)
	private String checkoutUrl;

	@Column(name = "checkout_expires_at")
	private Instant checkoutExpiresAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected ReservationPayment() {
	}

	public ReservationPayment(
			Reservation reservation,
			String provider,
			String providerReference,
			ReservationPaymentStatus status,
			BigDecimal amount,
			String currency) {
		this.reservation = reservation;
		this.provider = provider;
		this.providerReference = providerReference;
		this.status = status;
		this.amount = amount;
		this.currency = currency;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderReference() {
		return providerReference;
	}

	public ReservationPaymentStatus getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getCheckoutSessionId() {
		return checkoutSessionId;
	}

	public String getCheckoutUrl() {
		return checkoutUrl;
	}

	public Instant getCheckoutExpiresAt() {
		return checkoutExpiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setStatus(ReservationPaymentStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public void setProviderReference(String providerReference) {
		this.providerReference = providerReference;
		this.updatedAt = Instant.now();
	}

	public void setCheckoutSession(String checkoutSessionId, String checkoutUrl, Instant checkoutExpiresAt) {
		this.checkoutSessionId = checkoutSessionId;
		this.checkoutUrl = checkoutUrl;
		this.checkoutExpiresAt = checkoutExpiresAt;
		this.updatedAt = Instant.now();
	}
}
