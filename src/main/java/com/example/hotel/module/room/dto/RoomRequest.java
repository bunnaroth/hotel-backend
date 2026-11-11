package com.example.hotel.module.room.dto;

import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomRequest(
		@NotBlank @Size(max = 32) String roomNumber,
		@NotNull @Positive Long roomTypeId,
		@Size(max = 32) String floorLabel,
		@NotNull OperationalStatus operationalStatus,
		@NotNull HousekeepingStatus housekeepingStatus) {
}
