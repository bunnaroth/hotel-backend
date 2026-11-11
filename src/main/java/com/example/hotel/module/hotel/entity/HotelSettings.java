package com.example.hotel.module.hotel.entity;

import java.time.Instant;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotel_settings")
public class HotelSettings {

	@Id
	@Column(name = "settings_id")
	private Short id = 1;

	@Column(name = "hotel_name", nullable = false, length = 160)
	private String hotelName;

	@Column(name = "street_address", nullable = false, length = 300)
	private String streetAddress;

	@Column(name = "phone_number", length = 40)
	private String phoneNumber;

	@Column(name = "contact_email", length = 254)
	private String contactEmail;

	@Column(name = "time_zone", nullable = false, length = 100)
	private String timeZone;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected HotelSettings() {
	}

	public HotelSettings(
			String hotelName,
			String streetAddress,
			String phoneNumber,
			String contactEmail,
			ZoneId timeZone,
			String currencyCode) {
		update(hotelName, streetAddress, phoneNumber, contactEmail, timeZone, currencyCode);
	}

	public void update(
			String hotelName,
			String streetAddress,
			String phoneNumber,
			String contactEmail,
			ZoneId timeZone,
			String currencyCode) {
		this.hotelName = hotelName.trim();
		this.streetAddress = streetAddress.trim();
		this.phoneNumber = normalize(phoneNumber);
		this.contactEmail = normalize(contactEmail);
		this.timeZone = timeZone.getId();
		this.currencyCode = currencyCode;
		this.updatedAt = Instant.now();
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	public String getHotelName() {
		return hotelName;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
