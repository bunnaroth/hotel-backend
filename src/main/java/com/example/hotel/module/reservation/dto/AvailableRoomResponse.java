package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AvailableRoomResponse", description = "A bookable physical room and its remaining inventory for the requested dates.")
public record AvailableRoomResponse(
		@Schema(description = "Physical room database ID.", example = "101")
		Long roomId,
		@Schema(description = "Room-type database ID.", example = "1")
		Long roomTypeId,
		@Schema(description = "Room-type code.", example = "PVT-STD")
		String roomTypeCode,
		@Schema(description = "Room-type display name.", example = "Standard private room")
		String roomTypeName,
		@Schema(description = "PRIVATE or DORMITORY.", example = "PRIVATE")
		String category,
		@Schema(description = "Client-supported image key.", example = "PVT_SD")
		String imageKey,
		@Schema(description = "Maximum guests or beds in the room.", example = "2")
		short maxOccupancy,
		@Schema(description = "Base nightly rate.", example = "120.00")
		BigDecimal baseNightlyRate,
		@Schema(description = "Available inventory: one for a private room, remaining beds for a dormitory.", example = "1")
		int availableUnits) {
}
