package com.example.hotel.module.hotel.dto;

import java.time.ZoneId;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HotelSettingsRequest(
		@NotBlank @Size(max = 160) String hotelName,
		@NotBlank @Size(max = 300) String streetAddress,
		@Size(max = 40) String phoneNumber,
		@Email @Size(max = 254) String contactEmail,
		@NotNull ZoneId timeZone,
		@NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode) {
}
