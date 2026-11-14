package com.example.hotel.module.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReservationFolioResponse(
		UUID reservationId,
		String bookingReference,
		String currency,
		BigDecimal totalAmount,
		BigDecimal amountPaid,
		BigDecimal amountRefunded,
		BigDecimal outstandingBalance,
		List<FolioLine> lines) {

	public record FolioLine(
			String type,
			String description,
			BigDecimal amount,
			LocalDate effectiveDate) {
	}
}
