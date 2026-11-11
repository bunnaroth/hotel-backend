package com.example.hotel.module.room.dto;

import java.math.BigDecimal;

public record RoomTypeResponse(
		Long id,
		String code,
		String name,
		String description,
		short maxOccupancy,
		BigDecimal baseNightlyRate,
		boolean active) {
}
