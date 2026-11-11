package com.example.hotel.module.reservation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

	private final ReservationService reservationService;

	public ReservationExpiryScheduler(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@Scheduled(fixedDelayString = "${app.reservation.expiry-scan-delay-ms:60000}")
	public void expirePendingReservations() {
		reservationService.expirePendingReservations();
	}
}
