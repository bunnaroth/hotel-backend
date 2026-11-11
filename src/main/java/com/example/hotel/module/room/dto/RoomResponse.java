package com.example.hotel.module.room.dto;

import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;

public record RoomResponse(
		Long id,
		String roomNumber,
		Long roomTypeId,
		String roomTypeCode,
		String roomTypeName,
		String floorLabel,
		OperationalStatus operationalStatus,
		HousekeepingStatus housekeepingStatus) {
}
