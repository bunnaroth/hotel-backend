package com.example.hotel.module.reservation.controller;

import java.time.LocalDate;

import com.example.hotel.module.reservation.dto.AvailableRoomResponse;
import com.example.hotel.module.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Availability", description = "Public room availability search")
public class AvailabilityController {

	private final ReservationService reservationService;

	public AvailabilityController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping
	@Operation(
			summary = "Search available rooms",
			description = "Returns active room inventory for a stay date range, including remaining beds for dormitories.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching rooms returned"),
			@ApiResponse(responseCode = "400", description = "Invalid dates or query parameters",
					content = @Content)
	})
	public Page<AvailableRoomResponse> search(
			@Parameter(description = "Arrival date in ISO-8601 format; must be before checkOutDate.",
					example = "2032-03-10", required = true)
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
			@Parameter(description = "Departure date in ISO-8601 format; treated as exclusive.",
					example = "2032-03-12", required = true)
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
			@Parameter(description = "Limit results to a room type ID.", example = "1")
			@RequestParam(required = false) @Positive
			Long roomTypeId,
			@RequestParam(defaultValue = "1") @Positive
			@Parameter(description = "Number of guests", example = "1") int guestCount,
			@Parameter(description = "Pagination options: zero-based page, page size (default 20), and sort.")
			@PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
		return reservationService.searchAvailability(
				checkInDate, checkOutDate, roomTypeId, guestCount, pageable);
	}
}
