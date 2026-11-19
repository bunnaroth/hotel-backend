package com.example.hotel.module.reservation.controller;

import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse create(@Valid @RequestBody ReservationCreateRequest request) {
		return reservationService.createPendingReservation(request);
	}

}
