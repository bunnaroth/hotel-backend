package com.example.hotel.module.guest.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guest")
public class Guest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "guest_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "email", length = 254)
	private String email;

	@Column(name = "phone_number", length = 40)
	private String phoneNumber;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected Guest() {
	}

	public Guest(String firstName, String lastName, String email, String phoneNumber) {
		this.firstName = firstName.trim();
		this.lastName = lastName.trim();
		this.email = normalize(email);
		this.phoneNumber = normalize(phoneNumber);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}
}
