package com.example.hotel.module.room.dto;

import com.example.hotel.module.room.enums.OperationalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "HotelRoomStatusRequest", description = "Changes a room's operational status.")
public record HotelRoomStatusRequest(
		@Schema(description = "OUT_OF_SERVICE removes the room from availability; this is blocked while active reservations reference it.", example = "OUT_OF_SERVICE")
		@NotNull OperationalStatus operationalStatus) {
}
