package com.example.hotel.module.reservation.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class ReservationService {

	private static final char[] REFERENCE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final List<HousekeepingStatus> ASSIGNABLE_HOUSEKEEPING_STATUSES =
			List.of(HousekeepingStatus.CLEAN, HousekeepingStatus.INSPECTED);

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
		return roomRepository.findAvailableRooms(
						roomTypeId,
						guestCount,
						checkInDate,
						checkOutDate,
						OperationalStatus.AVAILABLE,
						ASSIGNABLE_HOUSEKEEPING_STATUSES,
						pageable)
				.map(room -> new AvailableRoomResponse(
						room.getId(),
						room.getRoomType().getId(),
						room.getRoomType().getCode(),
						room.getRoomType().getName(),
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
				OperationalStatus.AVAILABLE,
				ASSIGNABLE_HOUSEKEEPING_STATUSES);
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
		List<ReservationGuest> reservationGuests = new ArrayList<>();
		for (int index = 0; index < guests.size(); index++) {
			reservationGuests.add(new ReservationGuest(reservation, guests.get(index), index == 0));
		}
		reservationGuestRepository.saveAll(reservationGuests);
		List<RoomAllocation> allocations = rooms.stream()
				.map(room -> new RoomAllocation(reservation, room))
				.toList();
		roomAllocationRepository.saveAll(allocations);
		return toResponse(reservation, reservationGuests, allocations);
	}

	@Transactional(readOnly = true)
	public Page<ReservationResponse> searchReservations(
			String bookingReference,
			String guestName,
			ReservationStatus status,
			LocalDate checkInFrom,
			LocalDate checkInTo,
			Pageable pageable) {
		if (checkInFrom != null && checkInTo != null && checkInFrom.isAfter(checkInTo)) {
			throw new InvalidReservationRequestException("Check-in date range is invalid");
		}

		Specification<Reservation> specification = reservationSpecification(
				bookingReference, guestName, status, checkInFrom, checkInTo);
		Page<Reservation> reservations = reservationRepository.findAll(specification, pageable);
		return toResponsePage(reservations);
	}

	@Transactional(readOnly = true)
	public ReservationResponse getReservation(UUID reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException(reservationId));
		return toResponsePage(new PageImpl<>(List.of(reservation))).getContent().getFirst();
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

	private Page<ReservationResponse> toResponsePage(Page<Reservation> reservations) {
		List<Reservation> content = reservations.getContent();
		List<UUID> reservationIds = content.stream().map(Reservation::getId).toList();
		Map<UUID, List<ReservationGuest>> guestsByReservation = reservationIds.isEmpty()
				? Map.of()
				: reservationGuestRepository.findAllByReservation_IdIn(reservationIds).stream()
						.collect(Collectors.groupingBy(ReservationGuest::getReservationId));
		Map<UUID, List<RoomAllocation>> allocationsByReservation = reservationIds.isEmpty()
				? Map.of()
				: roomAllocationRepository.findAllByReservation_IdIn(reservationIds).stream()
						.collect(Collectors.groupingBy(RoomAllocation::getReservationId));

		List<ReservationResponse> responseContent = content.stream()
				.map(reservation -> toResponse(
						reservation,
						guestsByReservation.getOrDefault(reservation.getId(), List.of()).stream()
								.sorted(Comparator.comparing(ReservationGuest::isPrimary).reversed())
								.toList(),
						allocationsByReservation.getOrDefault(reservation.getId(), List.of())))
				.toList();
		return new PageImpl<>(responseContent, reservations.getPageable(), reservations.getTotalElements());
	}

	private static Specification<Reservation> reservationSpecification(
			String bookingReference,
			String guestName,
			ReservationStatus status,
			LocalDate checkInFrom,
			LocalDate checkInTo) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (bookingReference != null && !bookingReference.isBlank()) {
				String pattern = "%" + escapeLike(bookingReference.trim().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(builder.like(
						builder.lower(root.get("bookingReference")), pattern, '!'));
			}
			if (status != null) {
				predicates.add(builder.equal(root.get("status"), status));
			}
			if (checkInFrom != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("checkInDate"), checkInFrom));
			}
			if (checkInTo != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("checkInDate"), checkInTo));
			}
			if (guestName != null && !guestName.isBlank()) {
				String pattern = "%" + escapeLike(guestName.trim().toLowerCase(Locale.ROOT)) + "%";
				Subquery<UUID> matchingReservations = query.subquery(UUID.class);
				Root<ReservationGuest> guestLink = matchingReservations.from(ReservationGuest.class);
				Join<ReservationGuest, Guest> guest = guestLink.join("guest");
				matchingReservations.select(guestLink.get("reservation").get("id"))
						.where(builder.or(
								builder.like(builder.lower(guest.get("firstName")), pattern, '!'),
								builder.like(builder.lower(guest.get("lastName")), pattern, '!')));
				predicates.add(root.get("id").in(matchingReservations));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String value) {
		return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
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
