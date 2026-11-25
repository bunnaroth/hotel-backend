package com.example.hotel.module.room.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AVAILABLE rooms may be offered for booking; OCCUPIED and OUT_OF_SERVICE rooms are excluded from availability.")
public enum OperationalStatus {
	AVAILABLE,
	OCCUPIED,
	OUT_OF_SERVICE
}
