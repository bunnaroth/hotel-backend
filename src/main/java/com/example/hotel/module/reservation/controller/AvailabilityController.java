package com.example.hotel.module.reservation.controller;

import java.time.LocalDate;

import com.example.hotel.module.reservation.dto.AvailableRoomResponse;
import com.example.hotel.module.reservation.service.ReservationService;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

	private final ReservationService reservationService;

	public AvailabilityController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping
	public Page<AvailableRoomResponse> search(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
			@RequestParam(required = false) @Positive Long roomTypeId,
			@RequestParam(required = false) String floorLabel,
			@RequestParam(required = false) com.example.hotel.module.room.enums.OperationalStatus operationalStatus,
			@RequestParam(required = false) com.example.hotel.module.room.enums.HousekeepingStatus housekeepingStatus,
			@RequestParam(defaultValue = "1") @Positive int guestCount,
			@PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
		return reservationService.searchAvailability(
				checkInDate, checkOutDate, roomTypeId, guestCount, floorLabel, operationalStatus, housekeepingStatus, pageable);
	}
}
