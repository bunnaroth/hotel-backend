package com.example.hotel.module.reservation.controller;

import java.time.LocalDate;
import java.util.UUID;

import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationDashboardResponse;
import com.example.hotel.module.reservation.dto.ReservationFolioResponse;
import com.example.hotel.module.reservation.dto.ReservationPaymentRequest;
import com.example.hotel.module.reservation.dto.ReservationPaymentResponse;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/dashboard")
	public ReservationDashboardResponse dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return reservationService.getDashboard(date);
	}

	@GetMapping("/staff/dashboard")
	public ReservationDashboardResponse staffDashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return reservationService.getStaffDashboard(date);
	}

	@GetMapping("/occupancy")
	public ReservationDashboardResponse occupancy(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return reservationService.getDashboard(date);
	}

	@GetMapping("/{id}")
	public ReservationResponse get(@PathVariable UUID id) {
		return reservationService.getReservation(id);
	}

	@PatchMapping("/{id}/confirm")
	public ReservationResponse confirm(@PathVariable UUID id) {
		return reservationService.confirmReservation(id);
	}

	@PatchMapping("/{id}/cancel")
	public ReservationResponse cancel(@PathVariable UUID id) {
		return reservationService.cancelReservation(id);
	}

	@PatchMapping("/{id}/no-show")
	public ReservationResponse noShow(@PathVariable UUID id) {
		return reservationService.markNoShowReservation(id);
	}

	@PatchMapping("/{id}/check-in")
	public ReservationResponse checkIn(@PathVariable UUID id) {
		return reservationService.checkInReservation(id);
	}

	@PatchMapping("/{id}/check-out")
	public ReservationResponse checkOut(@PathVariable UUID id) {
		return reservationService.checkOutReservation(id);
	}

	@PostMapping("/{id}/payments/authorize")
	public ReservationPaymentResponse authorizePayment(@PathVariable UUID id, @Valid @RequestBody ReservationPaymentRequest request) {
		return reservationService.authorizeReservationPayment(id, request);
	}

	@PostMapping("/{id}/payments/capture")
	public ReservationPaymentResponse capturePayment(@PathVariable UUID id, @Valid @RequestBody ReservationPaymentRequest request) {
		return reservationService.captureReservationPayment(id, request);
	}

	@PostMapping("/{id}/payments/refund")
	public ReservationPaymentResponse refundPayment(@PathVariable UUID id, @Valid @RequestBody ReservationPaymentRequest request) {
		return reservationService.refundReservationPayment(id, request);
	}

	@GetMapping("/{id}/folio")
	public ReservationFolioResponse getFolio(@PathVariable UUID id) {
		return reservationService.getReservationFolio(id);
	}
}
