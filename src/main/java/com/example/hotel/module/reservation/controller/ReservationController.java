package com.example.hotel.module.reservation.controller;

import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Public reservation creation")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Create a pending reservation",
			description = "Creates a PENDING reservation and temporarily holds the selected inventory. Private rooms are allocated whole; dormitory selections reserve the requested number of beds. This endpoint does not charge a payment method.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Reservation created"),
			@ApiResponse(responseCode = "400", description = "Invalid reservation request",
					content = @Content),
			@ApiResponse(responseCode = "409", description = "One or more rooms are unavailable",
					content = @Content)
	})
	public ReservationResponse create(@Valid @RequestBody ReservationCreateRequest request) {
		return reservationService.createPendingReservation(request);
	}

}
