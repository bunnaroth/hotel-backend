package com.example.hotel.module.reservation.controller;

import java.util.Map;
import java.util.UUID;

import com.example.hotel.module.reservation.dto.ReservationLookupChallengeResponse;
import com.example.hotel.module.reservation.dto.ReservationLookupCodeRequest;
import com.example.hotel.module.reservation.dto.ReservationLookupRequest;
import com.example.hotel.module.reservation.dto.ReservationLookupResponse;
import com.example.hotel.module.reservation.service.InvalidReservationLookupException;
import com.example.hotel.module.reservation.service.ReservationLookupRateLimitException;
import com.example.hotel.module.reservation.service.ReservationLookupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reservation-lookups")
public class PublicReservationLookupController {

	private final ReservationLookupService reservationLookupService;

	public PublicReservationLookupController(ReservationLookupService reservationLookupService) {
		this.reservationLookupService = reservationLookupService;
	}

	@PostMapping
	public ResponseEntity<ReservationLookupChallengeResponse> requestCode(
			@Valid @RequestBody ReservationLookupRequest request,
			HttpServletRequest httpRequest) {
		ReservationLookupChallengeResponse response = reservationLookupService.requestCode(
				request.bookingReference(),
				request.email(),
				httpRequest.getRemoteAddr());
		return ResponseEntity.accepted()
				.cacheControl(CacheControl.noStore())
				.body(response);
	}

	@PostMapping("/{challengeId}/verify")
	public ResponseEntity<ReservationLookupResponse> verifyCode(
			@PathVariable UUID challengeId,
			@Valid @RequestBody ReservationLookupCodeRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(reservationLookupService.verifyCode(challengeId, request.code()));
	}

	@ExceptionHandler(InvalidReservationLookupException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCode() {
		return ResponseEntity.badRequest()
				.cacheControl(CacheControl.noStore())
				.body(Map.of("message", "The code is invalid or expired. Request a new code to try again."));
	}

	@ExceptionHandler(ReservationLookupRateLimitException.class)
	public ResponseEntity<Map<String, String>> handleRateLimit() {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.cacheControl(CacheControl.noStore())
				.header("Retry-After", "3600")
				.body(Map.of("message", "Too many lookup requests. Please try again later."));
	}
}
