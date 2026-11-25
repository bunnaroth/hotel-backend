package com.example.hotel.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ReservationLookupCodeRequest", description = "One-time verification code delivered to the primary guest's email.")
public record ReservationLookupCodeRequest(
		@Schema(description = "Email verification code.", example = "12345678")
		@NotBlank @Size(max = 16) String code) {
}
