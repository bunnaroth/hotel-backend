package com.example.hotel.module.room.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Image keys supported by the client. PVT_* keys are for private rooms, DRM_* keys for dormitories; ROOM_PLACEHOLDER works for either.")
public enum RoomImageKey {
	ROOM_PLACEHOLDER,
	PVT_SD,
	PVT_ST,
	PVT_DLX,
	PVT_FAM,
	DRM_4M,
	DRM_6M,
	DRM_6F,
	DRM_8M
}
