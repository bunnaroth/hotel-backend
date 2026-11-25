package com.example.hotel.module.room.dto;

import java.math.BigDecimal;

import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "RoomTypeRequest", description = "Create or replace the editable fields of a room type.")
public record RoomTypeRequest(
		@Schema(description = "Unique inventory code. It is trimmed and stored in upper case.", example = "PVT-STD")
		@NotBlank
		@Size(max = 32)
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$")
		String code,
		@Schema(description = "Display name for this room type.", example = "Standard private room")
		@NotBlank @Size(max = 100) String name,
		@Schema(description = "Inventory category.", example = "PRIVATE")
		@NotNull RoomCategory category,
		@Schema(description = "Image key supported by the client. The key must match the category; ROOM_PLACEHOLDER is accepted for either category.", example = "PVT_SD")
		@NotNull RoomImageKey imageKey,
		@Schema(description = "Optional room-type description.", example = "Private room with one double bed.")
		@Size(max = 1000) String description,
		@Schema(description = "Maximum guests or dormitory beds per room; must be between 1 and 32767.", example = "2")
		@NotNull @Positive @Max(Short.MAX_VALUE) Integer maxOccupancy,
		@Schema(description = "Maximum physical rooms belonging to this type. Must not be lower than its current room count.", example = "12")
		@NotNull @Positive Integer roomLimit,
		@Schema(description = "Positive nightly rate with at most two decimal places.", example = "120.00")
		@NotNull @DecimalMin(value = "0.00", inclusive = false)
		@Digits(integer = 10, fraction = 2) BigDecimal baseNightlyRate) {
}
