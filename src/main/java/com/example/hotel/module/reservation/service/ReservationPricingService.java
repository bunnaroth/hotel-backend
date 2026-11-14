package com.example.hotel.module.reservation.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationPricingService {

	private final RoomAllocationRepository roomAllocationRepository;

	public ReservationPricingService(RoomAllocationRepository roomAllocationRepository) {
		this.roomAllocationRepository = roomAllocationRepository;
	}

	@Transactional(readOnly = true)
	public BigDecimal calculateStayAmount(Reservation reservation) {
		long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
		BigDecimal total = BigDecimal.ZERO;
		for (RoomAllocation allocation : roomAllocationRepository.findAllByReservation_Id(reservation.getId())) {
			if (nights <= 0) {
				continue;
			}
			BigDecimal nightlyRate = allocation.getRoom() == null || allocation.getRoom().getRoomType() == null
					? BigDecimal.ZERO
					: allocation.getRoom().getRoomType().getBaseNightlyRate();
			total = total.add(nightlyRate.multiply(BigDecimal.valueOf(nights)));
		}
		return total;
	}
}
