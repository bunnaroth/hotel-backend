package com.example.hotel.module.room.controller;

import com.example.hotel.module.room.dto.RoomTypeRequest;
import com.example.hotel.module.room.dto.RoomTypeResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/room-types")
@PreAuthorize("hasAnyRole('OWNER', 'GENERAL_MANAGER', 'ADMINISTRATOR')")
public class RoomTypeController {

	private final RoomInventoryService service;

	public RoomTypeController(RoomInventoryService service) {
		this.service = service;
	}

	@GetMapping
	public Page<RoomTypeResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.listRoomTypes(pageable);
	}

	@PostMapping
	public RoomTypeResponse create(@Valid @RequestBody RoomTypeRequest request) {
		return service.createRoomType(request);
	}

	@PutMapping("/{id}")
	public RoomTypeResponse update(@PathVariable long id, @Valid @RequestBody RoomTypeRequest request) {
		return service.updateRoomType(id, request);
	}
}
