package com.example.hotel.module.reservation.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import com.example.hotel.module.guest.dto.GuestRequest;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.guest.repository.GuestRepository;
import com.example.hotel.module.reservation.dto.AvailableRoomResponse;
import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.dto.RoomSelectionRequest;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationGuestRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.enums.RoomCategory;
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
				RoomCategory.DORMITORY,
				OperationalStatus.AVAILABLE,
				pageable);
		List<Long> roomIds = availableRooms.getContent().stream().map(HotelRoom::getId).toList();
		Map<Long, Long> allocatedUnitsByRoom = new HashMap<>();
		if (!roomIds.isEmpty()) {
			roomAllocationRepository.sumActiveUnitsForRoomsAndDates(roomIds, checkInDate, checkOutDate)
					.forEach(usage -> allocatedUnitsByRoom.put(usage.getRoomId(), usage.getAllocatedUnits()));
		}
		return availableRooms.map(room -> new AvailableRoomResponse(
					room.getId(),
					room.getRoomType().getId(),
					room.getRoomType().getCode(),
					room.getRoomType().getName(),
					room.getRoomType().getCategory().name(),
					room.getRoomType().getImageKey().name(),
					room.getRoomType().getMaxOccupancy(),
					room.getRoomType().getBaseNightlyRate(),
					room.getRoomType().getCategory() == RoomCategory.DORMITORY
							? Math.toIntExact(room.getRoomType().getMaxOccupancy()
									- allocatedUnitsByRoom.getOrDefault(room.getId(), 0L))
							: 1));
	}

	@Transactional
	public ReservationResponse createPendingReservation(ReservationCreateRequest request) {
		validateDateRange(request.checkInDate(), request.checkOutDate());
		List<RoomSelectionRequest> requestedSelections = request.roomSelections();
		boolean useLegacyRoomIds = requestedSelections == null || requestedSelections.isEmpty();
		List<Long> selectedRoomIds;
		if (useLegacyRoomIds) {
			selectedRoomIds = request.roomIds() == null ? List.of() : request.roomIds();
		} else {
			selectedRoomIds = requestedSelections.stream().map(RoomSelectionRequest::roomId).toList();
		}
		if (selectedRoomIds.isEmpty()) {
			throw new InvalidReservationRequestException("At least one room selection is required");
		}
		if (new HashSet<>(selectedRoomIds).size() != selectedRoomIds.size()) {
			throw new InvalidReservationRequestException("A physical room may only appear once in room selections");
		}
		TreeSet<Long> orderedRoomIds = new TreeSet<>(selectedRoomIds);
		Map<Long, HotelRoom> roomsById = new HashMap<>();
		for (Long roomId : orderedRoomIds) {
			HotelRoom room = roomRepository.findByIdForUpdate(roomId)
					.filter(candidate -> candidate.getOperationalStatus() == OperationalStatus.AVAILABLE
							&& candidate.getRoomType().isActive())
					.orElseThrow(RoomUnavailableException::new);
			roomsById.put(roomId, room);
		}
		List<RoomSelectionRequest> roomSelections = useLegacyRoomIds
				? selectedRoomIds.stream().map(roomId -> {
					HotelRoom room = roomsById.get(roomId);
					int quantity = room.getRoomType().getCategory() == RoomCategory.DORMITORY
							? room.getRoomType().getMaxOccupancy()
							: 1;
					return new RoomSelectionRequest(roomId, quantity);
				}).toList()
				: requestedSelections;

		List<GuestRequest> additionalGuests = request.additionalGuests() == null
				? List.of()
				: request.additionalGuests();
		int guestCount = 1 + additionalGuests.size();
		int totalCapacity = 0;
		Map<Long, Integer> allocatedUnitsByRoom = new HashMap<>();
		for (RoomSelectionRequest selection : roomSelections) {
			HotelRoom room = roomsById.get(selection.roomId());
			long currentlyAllocated = roomAllocationRepository.sumActiveUnitsForRoomAndDates(
					room.getId(), request.checkInDate(), request.checkOutDate());
			if (room.getRoomType().getCategory() == RoomCategory.DORMITORY) {
				int availableBeds = room.getRoomType().getMaxOccupancy() - Math.toIntExact(currentlyAllocated);
				if (selection.quantity() > availableBeds) {
					throw new RoomUnavailableException();
				}
				totalCapacity += selection.quantity();
				allocatedUnitsByRoom.put(room.getId(), selection.quantity());
			} else {
				if (selection.quantity() != 1) {
					throw new InvalidReservationRequestException("Private rooms must be selected one room at a time");
				}
				if (currentlyAllocated > 0) {
					throw new RoomUnavailableException();
				}
				totalCapacity += room.getRoomType().getMaxOccupancy();
				allocatedUnitsByRoom.put(room.getId(), 1);
			}
		}
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
		List<RoomAllocation> allocations = roomSelections.stream()
				.map(selection -> new RoomAllocation(
						reservation,
						roomsById.get(selection.roomId()),
						allocatedUnitsByRoom.get(selection.roomId())))
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

	String newBookingReference() {
		int year = Year.now().getValue() % 100;
		for (int attempt = 0; attempt < 10; attempt++) {
			char[] suffix = new char[6];
			for (int index = 0; index < suffix.length; index++) {
				suffix[index] = REFERENCE_ALPHABET[RANDOM.nextInt(REFERENCE_ALPHABET.length)];
			}
			String reference = String.format(Locale.ROOT, "HTL-%02d-%s", year, new String(suffix));
			if (!reservationRepository.existsByBookingReference(reference)) {
				return reference;
			}
		}
		throw new IllegalStateException("Unable to generate a unique booking reference");
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
