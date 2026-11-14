package com.example.hotel.module.reservation.dto;

import java.time.LocalDate;
import java.util.List;

public record ReservationDashboardResponse(
		LocalDate date,
		long arrivals,
		long checkedIn,
		long occupiedRooms,
		long availableRooms,
		List<ReservationResponse> upcomingArrivals) {
}
