package com.example.hotel.module.room.dto;

import com.example.hotel.module.room.enums.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "HotelRoomUpdateRequest", description = "Update a room number and operational status. Room-type reassignment is intentionally unsupported.")
public record HotelRoomUpdateRequest(
		@Schema(description = "Unique room number. It is trimmed and stored in upper case.", example = "SD-102")
		@NotBlank
		@Size(max = 32)
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*$")
		String roomNumber,
		@Schema(description = "New operational status. OUT_OF_SERVICE is rejected while active reservations reference the room.", example = "AVAILABLE")
		@NotNull OperationalStatus operationalStatus) {
}
