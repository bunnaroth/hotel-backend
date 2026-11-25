package com.example.hotel.module.reservation.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationLookupChallenge;
import com.example.hotel.module.reservation.entity.ReservationPayment;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationLookupChallengeRepository;
import com.example.hotel.module.reservation.repository.ReservationPaymentRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.entity.RoomType;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationLookupServiceTest {

	@Test
	void verifiedLookupReturnsGroupedRoomCountsImagesAndLatestPaymentStatus() throws Exception {
		UUID challengeId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String code = "12345678";
		SecretKey signingKey = new SecretKeySpec(
				"reservation-lookup-test-key-0001".getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		ReservationLookupChallenge challenge = new ReservationLookupChallenge(
				challengeId,
				reservationId,
				"lookup-hash",
				"client-hash",
				hashCode(signingKey, challengeId, code),
				Instant.now().plusSeconds(600));
		Reservation reservation = mock(Reservation.class);
		when(reservation.getId()).thenReturn(reservationId);
		when(reservation.getBookingReference()).thenReturn("HTL-26-A8K92F");
		when(reservation.getCheckInDate()).thenReturn(LocalDate.of(2026, 12, 1));
		when(reservation.getCheckOutDate()).thenReturn(LocalDate.of(2026, 12, 4));
		when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);

		RoomType privateRoom = new RoomType(
				"STD", "Standard", RoomCategory.PRIVATE, RoomImageKey.PVT_SD,
				null, (short) 2, 10, BigDecimal.valueOf(100));
		RoomType dormitory = new RoomType(
				"DRM4", "Four-bed dormitory", RoomCategory.DORMITORY, RoomImageKey.DRM_4M,
				null, (short) 4, 10, BigDecimal.valueOf(25));
		List<RoomAllocation> allocations = List.of(
				new RoomAllocation(reservation, new HotelRoom("101", privateRoom, OperationalStatus.AVAILABLE), 1),
				new RoomAllocation(reservation, new HotelRoom("102", privateRoom, OperationalStatus.AVAILABLE), 1),
				new RoomAllocation(reservation, new HotelRoom("D1", dormitory, OperationalStatus.AVAILABLE), 3));
		ReservationPayment payment = mock(ReservationPayment.class);
		when(payment.getStatus()).thenReturn(ReservationPaymentStatus.CAPTURED);

		ReservationLookupChallengeRepository challengeRepository =
				mock(ReservationLookupChallengeRepository.class);
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		ReservationPaymentRepository paymentRepository = mock(ReservationPaymentRepository.class);
		RoomAllocationRepository roomAllocationRepository = mock(RoomAllocationRepository.class);
		when(challengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
		when(roomAllocationRepository.findAllByReservation_Id(reservationId)).thenReturn(allocations);
		when(paymentRepository.findFirstByReservation_IdOrderByCreatedAtDesc(reservationId))
				.thenReturn(Optional.of(payment));

		ReservationLookupService service = new ReservationLookupService(
				challengeRepository,
				reservationRepository,
				paymentRepository,
				roomAllocationRepository,
				mock(JavaMailSender.class),
				signingKey,
				"bookings@example.test",
				3,
				10);

		var response = service.verifyCode(challengeId, code);

		assertEquals("HTL-26-A8K92F", response.bookingReference());
		assertEquals(ReservationPaymentStatus.CAPTURED, response.paymentStatus());
		assertEquals(2, response.rooms().size());
		assertEquals("Four-bed dormitory", response.rooms().get(0).roomTypeName());
		assertEquals(RoomImageKey.DRM_4M, response.rooms().get(0).imageKey());
		assertEquals(3, response.rooms().get(0).quantity());
		assertEquals("Standard", response.rooms().get(1).roomTypeName());
		assertEquals(2, response.rooms().get(1).quantity());
	}

	private static String hashCode(SecretKey signingKey, UUID challengeId, String code) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		return HexFormat.of().formatHex(mac.doFinal(
				("lookup-code:" + challengeId + ":" + code).getBytes(StandardCharsets.UTF_8)));
	}
}
