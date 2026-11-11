package com.example.hotel.module.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomUpdateRequest(
		@NotBlank @Size(max = 32) String roomNumber,
		@NotNull @Positive Long roomTypeId,
		@Size(max = 32) String floorLabel) {
}
