package com.example.hotel.module.reservation.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.hotel.module.guest.dto.GuestRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationCreateRequest(
		@NotNull LocalDate checkInDate,
		@NotNull LocalDate checkOutDate,
		@NotNull @Valid GuestRequest primaryGuest,
		@Valid List<@NotNull GuestRequest> additionalGuests,
		@NotEmpty List<@NotNull @Positive Long> roomIds) {

	@AssertTrue(message = "Check-in date must be before check-out date")
	public boolean isDateRangeValid() {
		return checkInDate != null && checkOutDate != null && checkInDate.isBefore(checkOutDate);
	}
}
