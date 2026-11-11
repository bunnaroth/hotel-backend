package com.example.hotel.module.room.controller;

import com.example.hotel.module.room.dto.RoomRequest;
import com.example.hotel.module.room.dto.RoomResponse;
import com.example.hotel.module.room.dto.RoomUpdateRequest;
import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.service.RoomInventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@PreAuthorize("hasAnyRole('OWNER', 'GENERAL_MANAGER', 'ADMINISTRATOR')")
public class RoomController {

	private final RoomInventoryService service;

	public RoomController(RoomInventoryService service) {
		this.service = service;
	}

	@GetMapping
	public Page<RoomResponse> search(
			@RequestParam(required = false) Long roomTypeId,
			@RequestParam(required = false) OperationalStatus operationalStatus,
			@RequestParam(required = false) HousekeepingStatus housekeepingStatus,
			@PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
		return service.searchRooms(roomTypeId, operationalStatus, housekeepingStatus, pageable);
	}

	@PostMapping
	public RoomResponse create(@Valid @RequestBody RoomRequest request) {
		return service.createRoom(request);
	}

	@PutMapping("/{id}")
	public RoomResponse update(@PathVariable long id, @Valid @RequestBody RoomUpdateRequest request) {
		return service.updateRoom(id, request);
	}
}
