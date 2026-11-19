package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;

public record AvailableRoomResponse(
		Long roomId,
		Long roomTypeId,
		String roomTypeCode,
		String roomTypeName,
		String category,
		String imageKey,
		short maxOccupancy,
		BigDecimal baseNightlyRate) {
}
