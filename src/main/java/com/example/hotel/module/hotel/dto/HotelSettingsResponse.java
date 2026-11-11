package com.example.hotel.module.hotel.dto;

import java.time.Instant;

public record HotelSettingsResponse(
		String hotelName,
		String streetAddress,
		String phoneNumber,
		String contactEmail,
		String timeZone,
		String currencyCode,
		Instant updatedAt) {
}
