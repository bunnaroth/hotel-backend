package com.example.hotel.module.room.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "PRIVATE inventory is allocated as a whole room; DORMITORY inventory is allocated by bed.")
public enum RoomCategory {
	PRIVATE,
	DORMITORY
}
