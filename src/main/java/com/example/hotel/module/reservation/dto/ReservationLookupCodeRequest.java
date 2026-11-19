package com.example.hotel.module.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationLookupCodeRequest(
		@NotBlank @Size(max = 16) String code) {
}
