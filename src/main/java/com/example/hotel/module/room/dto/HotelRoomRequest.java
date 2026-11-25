package com.example.hotel.module.room.dto;

import com.example.hotel.module.room.enums.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "HotelRoomRequest", description = "Create a physical room under an active room type.")
public record HotelRoomRequest(
		@Schema(description = "Unique room number. It is trimmed and stored in upper case.", example = "SD-101")
		@NotBlank
		@Size(max = 32)
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*$")
		String roomNumber,
		@Schema(description = "ID of an active room type that has not reached its room limit.", example = "1")
		@NotNull @Positive Long roomTypeId,
		@Schema(description = "Initial operational status.", example = "AVAILABLE")
		@NotNull OperationalStatus operationalStatus) {
}
