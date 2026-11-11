package com.example.hotel.module.guest.controller;

import com.example.hotel.module.guest.dto.GuestResponse;
import com.example.hotel.module.guest.service.GuestService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/guests")
public class GuestController {

	private final GuestService guestService;

	public GuestController(GuestService guestService) {
		this.guestService = guestService;
	}

	@GetMapping
	public Page<GuestResponse> search(
			@RequestParam @NotBlank @Size(min = 2, max = 254) String query,
			@PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
		return guestService.search(query, pageable);
	}
}
