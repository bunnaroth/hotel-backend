package com.example.hotel.module.reservation.controller;

import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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

	@GetMapping
	public Page<ReservationResponse> search(
			@RequestParam(required = false) String bookingReference,
			@RequestParam(required = false) String guestName,
			@RequestParam(required = false) ReservationStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
			@PageableDefault(size = 20, sort = "checkInDate") Pageable pageable) {
		return reservationService.searchReservations(
				bookingReference, guestName, status, checkInFrom, checkInTo, pageable);
	}

	@GetMapping("/{id}")
	public ReservationResponse get(@PathVariable UUID id) {
		return reservationService.getReservation(id);
	}
}
