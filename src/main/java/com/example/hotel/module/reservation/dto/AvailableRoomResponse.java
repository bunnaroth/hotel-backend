package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;

public record AvailableRoomResponse(
		Long roomId,
		Long roomTypeId,
		String roomTypeCode,
		String roomTypeName,
		short maxOccupancy,
		BigDecimal baseNightlyRate) {
}
