package com.example.hotel.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ReservationLookupRequest", description = "Requests an email verification code for account-free reservation lookup.")
public record ReservationLookupRequest(
		@Schema(description = "Booking reference shown when the reservation was created.", example = "HTL-26-A8K92F")
		@NotBlank @Size(max = 24) String bookingReference,
		@Schema(description = "Primary guest email address associated with the reservation.", example = "pat@example.test")
		@NotBlank @Email @Size(max = 254) String email) {
}
