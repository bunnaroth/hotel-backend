package com.example.hotel.module.room.dto;

import java.time.Instant;

import com.example.hotel.module.room.enums.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HotelRoomResponse", description = "Physical room inventory record.")
public record HotelRoomResponse(
		@Schema(description = "Physical room database ID.", example = "101")
		Long id,
		@Schema(description = "Room number.", example = "SD-101")
		String roomNumber,
		@Schema(description = "Linked room-type database ID.", example = "1")
		Long roomTypeId,
		@Schema(description = "Linked room-type code.", example = "PVT-STD")
		String roomTypeCode,
		@Schema(description = "Linked room-type name.", example = "Standard private room")
		String roomTypeName,
		@Schema(description = "Operational state.", example = "AVAILABLE")
		OperationalStatus operationalStatus,
		@Schema(description = "Creation timestamp in UTC.", example = "2026-11-23T12:00:00Z")
		Instant createdAt,
		@Schema(description = "Last update timestamp in UTC.", example = "2026-11-23T12:00:00Z")
		Instant updatedAt) {
}
