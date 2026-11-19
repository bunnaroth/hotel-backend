package com.example.hotel.module.reservation.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.hotel.module.guest.dto.GuestRequest;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.guest.repository.GuestRepository;
import com.example.hotel.module.reservation.dto.AvailableRoomResponse;
import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationGuestRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

	private static final char[] REFERENCE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();
	private final GuestRepository guestRepository;
	private final RoomRepository roomRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationGuestRepository reservationGuestRepository;
	private final RoomAllocationRepository roomAllocationRepository;
	private final Duration pendingTtl;

	public ReservationService(
			GuestRepository guestRepository,
			RoomRepository roomRepository,
			ReservationRepository reservationRepository,
			ReservationGuestRepository reservationGuestRepository,
			RoomAllocationRepository roomAllocationRepository,
			@Value("${app.reservation.pending-ttl}") Duration pendingTtl) {
		if (pendingTtl.isNegative() || pendingTtl.isZero()) {
			throw new IllegalArgumentException("Reservation pending TTL must be positive");
		}
		this.guestRepository = guestRepository;
		this.roomRepository = roomRepository;
		this.reservationRepository = reservationRepository;
		this.reservationGuestRepository = reservationGuestRepository;
		this.roomAllocationRepository = roomAllocationRepository;
		this.pendingTtl = pendingTtl;
	}

	@Transactional(readOnly = true)
	public Page<AvailableRoomResponse> searchAvailability(
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Long roomTypeId,
			int guestCount,
			Pageable pageable) {
		validateDateRange(checkInDate, checkOutDate);
		if (guestCount < 1) {
			throw new InvalidReservationRequestException("Guest count must be positive");
		}
		Page<HotelRoom> availableRooms = roomRepository.findAvailableRooms(
				roomTypeId,
				guestCount,
				checkInDate,
				checkOutDate,
				OperationalStatus.AVAILABLE,
				pageable);
		return availableRooms.map(room -> new AvailableRoomResponse(
					room.getId(),
					room.getRoomType().getId(),
					room.getRoomType().getCode(),
					room.getRoomType().getName(),
					room.getRoomType().getCategory().name(),
					room.getRoomType().getImageKey().name(),
					room.getRoomType().getMaxOccupancy(),
					room.getRoomType().getBaseNightlyRate()));
	}

	@Transactional
	public ReservationResponse createPendingReservation(ReservationCreateRequest request) {
		validateDateRange(request.checkInDate(), request.checkOutDate());
		if (new HashSet<>(request.roomIds()).size() != request.roomIds().size()) {
			throw new InvalidReservationRequestException("A room may only be selected once");
		}

		List<GuestRequest> additionalGuests = request.additionalGuests() == null
				? List.of()
				: request.additionalGuests();
		int guestCount = 1 + additionalGuests.size();
		List<HotelRoom> rooms = roomRepository.findAvailableRooms(
				request.roomIds(),
				request.checkInDate(),
				request.checkOutDate(),
				OperationalStatus.AVAILABLE);
		if (rooms.size() != request.roomIds().size()) {
			throw new RoomUnavailableException();
		}
		int totalCapacity = rooms.stream()
				.mapToInt(room -> room.getRoomType().getMaxOccupancy())
				.sum();
		if (totalCapacity < guestCount) {
			throw new InvalidReservationRequestException("Selected rooms do not have enough guest capacity");
		}

		Guest primaryGuest = guestRepository.save(toGuest(request.primaryGuest()));
		List<Guest> guests = new ArrayList<>();
		guests.add(primaryGuest);
		for (GuestRequest guestRequest : additionalGuests) {
			guests.add(guestRepository.save(toGuest(guestRequest)));
		}

		Instant now = Instant.now();
		Reservation reservation = reservationRepository.save(new Reservation(
				newBookingReference(),
				primaryGuest,
				request.checkInDate(),
				request.checkOutDate(),
				now.plus(pendingTtl)));
		List<ReservationGuest> reservationGuests = guests.stream()
				.map(guest -> new ReservationGuest(reservation, guest))
				.toList();
		reservationGuestRepository.saveAll(reservationGuests);
		List<RoomAllocation> allocations = rooms.stream()
				.map(room -> new RoomAllocation(reservation, room))
				.toList();
		roomAllocationRepository.saveAll(allocations);
		return toResponse(reservation, reservationGuests, allocations);
	}

	@Transactional
	public int expirePendingReservations() {
		Instant now = Instant.now();
		List<Reservation> expiredCandidates = reservationRepository
				.findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(ReservationStatus.PENDING, now);
		int expiredCount = 0;
		for (Reservation candidate : expiredCandidates) {
			Reservation reservation = reservationRepository.findByIdForUpdate(candidate.getId()).orElse(null);
			if (reservation == null || !reservation.isPendingAndExpired(now)) {
				continue;
			}
			reservation.expire(now);
			roomAllocationRepository.findAllByReservation_IdAndActiveTrue(reservation.getId())
					.forEach(RoomAllocation::release);
			expiredCount++;
		}
		return expiredCount;
	}

	private static Guest toGuest(GuestRequest request) {
		return new Guest(request.firstName(), request.lastName(), request.email(), request.phoneNumber());
	}

	private static ReservationResponse toResponse(
			Reservation reservation,
			List<ReservationGuest> guests,
			List<RoomAllocation> allocations) {
		return new ReservationResponse(
				reservation.getId(),
				reservation.getBookingReference(),
				reservation.getCheckInDate(),
				reservation.getCheckOutDate(),
				reservation.getStatus(),
				reservation.getExpiresAt(),
				guests.stream()
						.map(guest -> new ReservationResponse.GuestSummary(guest.getGuestName(), guest.isPrimary()))
						.toList(),
				allocations.stream().map(RoomAllocation::getRoomId).sorted().toList());
	}

	private static void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
		if (checkInDate == null || checkOutDate == null || !checkInDate.isBefore(checkOutDate)) {
			throw new InvalidReservationRequestException("Check-in date must be before check-out date");
		}
	}

	private static String newBookingReference() {
		char[] reference = new char[16];
		for (int index = 0; index < reference.length; index++) {
			reference[index] = REFERENCE_ALPHABET[RANDOM.nextInt(REFERENCE_ALPHABET.length)];
		}
		return new String(reference).toUpperCase(Locale.ROOT);
	}

	public static class InvalidReservationRequestException extends RuntimeException {

		public InvalidReservationRequestException(String message) {
			super(message);
		}
	}

	public static class RoomUnavailableException extends RuntimeException {

		public RoomUnavailableException() {
			super("One or more selected rooms are not available for the requested dates");
		}
	}

	public static class ReservationNotFoundException extends RuntimeException {

		public ReservationNotFoundException(UUID reservationId) {
			super("Reservation not found: " + reservationId);
		}
	}
}
