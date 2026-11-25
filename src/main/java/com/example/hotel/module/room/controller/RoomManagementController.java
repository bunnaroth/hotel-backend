package com.example.hotel.module.room.controller;

import java.net.URI;
import java.util.List;

import com.example.hotel.module.room.dto.HotelRoomRequest;
import com.example.hotel.module.room.dto.HotelRoomResponse;
import com.example.hotel.module.room.dto.HotelRoomStatusRequest;
import com.example.hotel.module.room.dto.HotelRoomUpdateRequest;
import com.example.hotel.module.room.dto.RoomTypeActiveRequest;
import com.example.hotel.module.room.dto.RoomTypeRequest;
import com.example.hotel.module.room.dto.RoomTypeResponse;
import com.example.hotel.module.room.service.RoomManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "adminBasicAuth")
@Tag(
		name = "Admin room inventory",
		description = "HTTP Basic-protected inventory management. No hard-delete operations are provided; use inactive room types or OUT_OF_SERVICE rooms.")
public class RoomManagementController {

	private final RoomManagementService roomManagementService;

	public RoomManagementController(RoomManagementService roomManagementService) {
		this.roomManagementService = roomManagementService;
	}

	@GetMapping("/room-types")
	@Operation(
			summary = "List room types",
			description = "Returns all room types, including inactive types, ordered by code.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room types returned"),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content)
	})
	public List<RoomTypeResponse> listRoomTypes() {
		return roomManagementService.listRoomTypes();
	}

	@GetMapping("/room-types/{id}")
	@Operation(summary = "Get a room type", description = "Returns one room type by its database ID.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room type returned"),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room type does not exist", content = @Content)
	})
	public RoomTypeResponse getRoomType(@Parameter(description = "Room-type database ID.", example = "1")
			@PathVariable Long id) {
		return roomManagementService.getRoomType(id);
	}

	@PostMapping("/room-types")
	@Operation(
			summary = "Create a room type",
			description = "Creates an active room type. Its code must be unique, its image key must match its category, and its nightly rate and capacity must be positive.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Room type created"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "409", description = "Room-type code conflicts with existing inventory", content = @Content)
	})
	public ResponseEntity<RoomTypeResponse> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
		RoomTypeResponse created = roomManagementService.createRoomType(request);
		return ResponseEntity.created(URI.create("/api/admin/room-types/" + created.id())).body(created);
	}

	@PutMapping("/room-types/{id}")
	@Operation(
			summary = "Update a room type",
			description = "Replaces editable room-type fields. Category, capacity, and nightly rate cannot change while the type has active reservations; roomLimit cannot be lower than its current room count.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room type updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room type does not exist", content = @Content),
			@ApiResponse(responseCode = "409", description = "Update conflicts with existing inventory or active reservations", content = @Content)
	})
	public RoomTypeResponse updateRoomType(
			@Parameter(description = "Room-type database ID.", example = "1") @PathVariable Long id,
			@Valid @RequestBody RoomTypeRequest request) {
		return roomManagementService.updateRoomType(id, request);
	}

	@PatchMapping("/room-types/{id}/active")
	@Operation(
			summary = "Activate or deactivate a room type",
			description = "Setting active to false removes this type from future availability results without deleting it or its reservation history.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room-type active state updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room type does not exist", content = @Content)
	})
	public RoomTypeResponse setRoomTypeActive(
			@Parameter(description = "Room-type database ID.", example = "1") @PathVariable Long id,
			@Valid @RequestBody RoomTypeActiveRequest request) {
		return roomManagementService.setRoomTypeActive(id, request);
	}

	@GetMapping("/rooms")
	@Operation(summary = "List physical rooms", description = "Returns all physical rooms ordered by room number.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Rooms returned"),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content)
	})
	public List<HotelRoomResponse> listRooms() {
		return roomManagementService.listRooms();
	}

	@GetMapping("/rooms/{id}")
	@Operation(summary = "Get a physical room", description = "Returns one physical room and its linked room-type summary.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room returned"),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room does not exist", content = @Content)
	})
	public HotelRoomResponse getRoom(@Parameter(description = "Physical room database ID.", example = "101")
			@PathVariable Long id) {
		return roomManagementService.getRoom(id);
	}

	@PostMapping("/rooms")
	@Operation(
			summary = "Create a physical room",
			description = "Creates a room for an active room type. The type must have roomLimit capacity remaining, and room numbers must be unique.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Room created"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room type does not exist", content = @Content),
			@ApiResponse(responseCode = "409", description = "Room number conflicts, the type is inactive, or its room limit is reached", content = @Content)
	})
	public ResponseEntity<HotelRoomResponse> createRoom(@Valid @RequestBody HotelRoomRequest request) {
		HotelRoomResponse created = roomManagementService.createRoom(request);
		return ResponseEntity.created(URI.create("/api/admin/rooms/" + created.id())).body(created);
	}

	@PutMapping("/rooms/{id}")
	@Operation(
			summary = "Update a physical room",
			description = "Updates its room number and operational status. Room-type reassignment is intentionally unsupported. An active reservation prevents setting the room OUT_OF_SERVICE.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room does not exist", content = @Content),
			@ApiResponse(responseCode = "409", description = "Room number conflicts or active reservations prevent the requested status", content = @Content)
	})
	public HotelRoomResponse updateRoom(
			@Parameter(description = "Physical room database ID.", example = "101") @PathVariable Long id,
			@Valid @RequestBody HotelRoomUpdateRequest request) {
		return roomManagementService.updateRoom(id, request);
	}

	@PatchMapping("/rooms/{id}/operational-status")
	@Operation(
			summary = "Change a room's operational status",
			description = "Use OUT_OF_SERVICE to deactivate a room without deleting it. This change is rejected while active reservations reference the room.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Room status updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
			@ApiResponse(responseCode = "401", description = "Admin credentials are missing or invalid", content = @Content),
			@ApiResponse(responseCode = "404", description = "Room does not exist", content = @Content),
			@ApiResponse(responseCode = "409", description = "Active reservations prevent taking the room out of service", content = @Content)
	})
	public HotelRoomResponse setRoomOperationalStatus(
			@Parameter(description = "Physical room database ID.", example = "101") @PathVariable Long id,
			@Valid @RequestBody HotelRoomStatusRequest request) {
		return roomManagementService.setRoomOperationalStatus(id, request);
	}
}
