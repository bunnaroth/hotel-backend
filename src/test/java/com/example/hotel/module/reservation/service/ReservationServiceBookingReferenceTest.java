package com.example.hotel.module.reservation.service;

import java.time.Duration;

import com.example.hotel.module.guest.repository.GuestRepository;
import com.example.hotel.module.reservation.repository.ReservationGuestRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceBookingReferenceTest {

	@Test
	void createsReferenceWithHotelYearAndSixCharacterSuffix() {
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		when(reservationRepository.existsByBookingReference(anyString())).thenReturn(false);
		ReservationService service = createService(reservationRepository);

		assertTrue(service.newBookingReference().matches("HTL-\\d{2}-[0-9A-HJKMNP-TV-Z]{6}"));
	}

	@Test
	void retriesWhenRandomReferenceAlreadyExists() {
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		when(reservationRepository.existsByBookingReference(anyString())).thenReturn(true, false);
		ReservationService service = createService(reservationRepository);

		assertTrue(service.newBookingReference().matches("HTL-\\d{2}-[0-9A-HJKMNP-TV-Z]{6}"));
		verify(reservationRepository, times(2)).existsByBookingReference(anyString());
	}

	private static ReservationService createService(ReservationRepository reservationRepository) {
		return new ReservationService(
				mock(GuestRepository.class),
				mock(RoomRepository.class),
				reservationRepository,
				mock(ReservationGuestRepository.class),
				mock(RoomAllocationRepository.class),
				Duration.ofMinutes(15));
	}
}
