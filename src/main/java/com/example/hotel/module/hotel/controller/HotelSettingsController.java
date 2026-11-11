package com.example.hotel.module.hotel.controller;

import com.example.hotel.module.hotel.dto.HotelSettingsRequest;
import com.example.hotel.module.hotel.dto.HotelSettingsResponse;
import com.example.hotel.module.hotel.service.HotelSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotel/settings")
@PreAuthorize("hasAnyRole('OWNER', 'GENERAL_MANAGER', 'ADMINISTRATOR')")
public class HotelSettingsController {

	private final HotelSettingsService service;

	public HotelSettingsController(HotelSettingsService service) {
		this.service = service;
	}

	@GetMapping
	public HotelSettingsResponse getSettings() {
		return service.getSettings();
	}

	@PutMapping
	public HotelSettingsResponse saveSettings(@Valid @RequestBody HotelSettingsRequest request) {
		return service.saveSettings(request);
	}
}
