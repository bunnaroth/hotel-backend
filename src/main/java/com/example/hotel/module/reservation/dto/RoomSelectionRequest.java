package com.example.hotel.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "RoomSelectionRequest", description = "Selects a physical room and the number of units to reserve.")
public record RoomSelectionRequest(
		@Schema(description = "Physical room database ID.", example = "101")
		@NotNull @Positive Long roomId,
		@Schema(description = "Number of rooms or dormitory beds. Private rooms require quantity 1.", example = "1")
		@NotNull @Positive Integer quantity) {
}
