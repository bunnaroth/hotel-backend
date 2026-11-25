package com.example.hotel.module.reservation.dto;

import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;

public record ReservationLookupRoomResponse(
		String roomTypeCode,
		String roomTypeName,
		RoomCategory category,
		RoomImageKey imageKey,
		int quantity) {
}
