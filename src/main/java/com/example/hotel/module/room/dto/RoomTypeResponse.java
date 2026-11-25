package com.example.hotel.module.room.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoomTypeResponse", description = "Room-type inventory and booking configuration.")
public record RoomTypeResponse(
		@Schema(description = "Room-type database ID.", example = "1")
		Long id,
		@Schema(description = "Unique inventory code.", example = "PVT-STD")
		String code,
		@Schema(description = "Display name.", example = "Standard private room")
		String name,
		@Schema(description = "Inventory category.", example = "PRIVATE")
		RoomCategory category,
		@Schema(description = "Client-supported image key.", example = "PVT_SD")
		RoomImageKey imageKey,
		@Schema(description = "Optional description.")
		String description,
		@Schema(description = "Maximum guests or beds per physical room.", example = "2")
		short maxOccupancy,
		@Schema(description = "Maximum configured number of rooms for this type.", example = "12")
		int roomLimit,
		@Schema(description = "Nightly rate.", example = "120.00")
		BigDecimal baseNightlyRate,
		@Schema(description = "Inactive room types are excluded from public availability.", example = "true")
		boolean active,
		@Schema(description = "Creation timestamp in UTC.", example = "2026-11-23T12:00:00Z")
		Instant createdAt,
		@Schema(description = "Last update timestamp in UTC.", example = "2026-11-23T12:00:00Z")
		Instant updatedAt) {
}
