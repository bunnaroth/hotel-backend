package com.example.hotel.module.room.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomTypeRequest(
		@NotBlank @Size(max = 32) String code,
		@NotBlank @Size(max = 100) String name,
		@Size(max = 1000) String description,
		@Positive short maxOccupancy,
		@NotNull @DecimalMin(value = "0.01") BigDecimal baseNightlyRate,
		boolean active) {
}
