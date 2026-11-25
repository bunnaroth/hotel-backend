package com.example.hotel.module.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RoomTypeActiveRequest", description = "Changes whether a room type is offered for future availability searches.")
public record RoomTypeActiveRequest(
		@Schema(description = "Set false to deactivate or true to reactivate the room type.", example = "false")
		@NotNull Boolean active) {
}
