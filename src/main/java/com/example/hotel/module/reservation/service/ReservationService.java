package com.example.hotel.module.reservation.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.hotel.module.guest.dto.GuestRequest;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.guest.repository.GuestRepository;
import com.example.hotel.module.hotel.entity.HotelSettings;
import com.example.hotel.module.hotel.repository.HotelSettingsRepository;
import com.example.hotel.module.reservation.dto.AvailableRoomResponse;
import com.example.hotel.module.reservation.dto.ReservationCreateRequest;
import com.example.hotel.module.reservation.dto.ReservationDashboardResponse;
import com.example.hotel.module.reservation.dto.ReservationFolioResponse;
import com.example.hotel.module.reservation.dto.ReservationPaymentRequest;
import com.example.hotel.module.reservation.dto.ReservationPaymentResponse;
import com.example.hotel.module.reservation.dto.ReservationResponse;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.entity.ReservationPayment;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationGuestRepository;
import com.example.hotel.module.reservation.repository.ReservationPaymentRepository;
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
	private final ReservationPaymentRepository reservationPaymentRepository;
	private final RoomAllocationRepository roomAllocationRepository;
	private final HotelSettingsRepository hotelSettingsRepository;
	private final ReservationPricingService pricingService;
	private final Duration pendingTtl;

	public ReservationService(
			GuestRepository guestRepository,
			RoomRepository roomRepository,
			ReservationRepository reservationRepository,
			ReservationGuestRepository reservationGuestRepository,
			ReservationPaymentRepository reservationPaymentRepository,
			RoomAllocationRepository roomAllocationRepository,
			HotelSettingsRepository hotelSettingsRepository,
			ReservationPricingService pricingService,
			@Value("${app.reservation.pending-ttl}") Duration pendingTtl) {
		if (pendingTtl.isNegative() || pendingTtl.isZero()) {
			throw new IllegalArgumentException("Reservation pending TTL must be positive");
		}
		this.guestRepository = guestRepository;
		this.roomRepository = roomRepository;
		this.reservationRepository = reservationRepository;
		this.reservationGuestRepository = reservationGuestRepository;
		this.reservationPaymentRepository = reservationPaymentRepository;
		this.roomAllocationRepository = roomAllocationRepository;
		this.hotelSettingsRepository = hotelSettingsRepository;
		this.pricingService = pricingService;
		this.pendingTtl = pendingTtl;
	}

	@Transactional(readOnly = true)
	public Page<AvailableRoomResponse> searchAvailability(
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Long roomTypeId,
			int guestCount,
			String floorLabel,
			OperationalStatus operationalStatus,
			HousekeepingStatus housekeepingStatus,
			Pageable pageable) {
		validateDateRange(checkInDate, checkOutDate);
		if (guestCount < 1) {
			throw new InvalidReservationRequestException("Guest count must be positive");
		}
		OperationalStatus effectiveOperationalStatus = operationalStatus == null ? OperationalStatus.AVAILABLE : operationalStatus;
		List<HousekeepingStatus> effectiveHousekeepingStatuses = housekeepingStatus == null
				? ASSIGNABLE_HOUSEKEEPING_STATUSES
				: List.of(housekeepingStatus);
		return roomRepository.findAvailableRooms(
						roomTypeId,
						guestCount,
						checkInDate,
						checkOutDate,
						effectiveOperationalStatus,
						effectiveHousekeepingStatuses,
						floorLabel,
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

	@Transactional
	public ReservationPaymentResponse authorizeReservationPayment(UUID reservationId, ReservationPaymentRequest request) {
		Reservation reservation = findReservation(reservationId);
		ensureReservationAcceptsPayment(reservation);
		ReservationPayment latest = reservationPaymentRepository.findFirstByReservation_IdOrderByCreatedAtDesc(reservationId)
				.orElse(null);
		if (latest != null && latest.getStatus() == ReservationPaymentStatus.AUTHORIZED) {
			return toPaymentResponse(latest, reservationId);
		}
		if (latest != null && latest.getStatus() == ReservationPaymentStatus.CAPTURED) {
			throw new IllegalStateException("Payment has already been captured for this reservation");
		}
		ReservationPayment payment = createPayment(reservation, request, ReservationPaymentStatus.AUTHORIZED);
		return toPaymentResponse(payment, reservationId);
	}

	@Transactional
	public ReservationPaymentResponse captureReservationPayment(UUID reservationId, ReservationPaymentRequest request) {
		Reservation reservation = findReservation(reservationId);
		ensureReservationAcceptsPayment(reservation);
		ReservationPayment latest = reservationPaymentRepository.findFirstByReservation_IdOrderByCreatedAtDesc(reservationId)
				.orElse(null);
		if (latest != null && latest.getStatus() == ReservationPaymentStatus.CAPTURED) {
			return toPaymentResponse(latest, reservationId);
		}
		if (latest != null && latest.getStatus() == ReservationPaymentStatus.REFUNDED) {
			throw new IllegalStateException("Refunded reservations cannot be captured again");
		}
		ReservationPayment payment = createPayment(reservation, request, ReservationPaymentStatus.CAPTURED);
		return toPaymentResponse(payment, reservationId);
	}

	@Transactional
	public ReservationPaymentResponse refundReservationPayment(UUID reservationId, ReservationPaymentRequest request) {
		Reservation reservation = findReservation(reservationId);
		if (reservation.getStatus() == ReservationStatus.PENDING) {
			throw new IllegalStateException("Pending reservations cannot be refunded before confirmation");
		}
		ReservationPayment latest = reservationPaymentRepository.findFirstByReservation_IdOrderByCreatedAtDesc(reservationId)
				.orElse(null);
		if (latest != null && latest.getStatus() == ReservationPaymentStatus.REFUNDED) {
			return toPaymentResponse(latest, reservationId);
		}
		ReservationPayment payment = createPayment(reservation, request, ReservationPaymentStatus.REFUNDED);
		return toPaymentResponse(payment, reservationId);
	}

	@Transactional(readOnly = true)
	public ReservationFolioResponse getReservationFolio(UUID reservationId) {
		Reservation reservation = findReservation(reservationId);
		List<ReservationPayment> payments = reservationPaymentRepository.findAllByReservation_IdOrderByCreatedAtAsc(reservationId);
		BigDecimal totalStayAmount = pricingService.calculateStayAmount(reservation);
		BigDecimal amountPaid = payments.stream()
				.filter(payment -> payment.getStatus() == ReservationPaymentStatus.CAPTURED)
				.map(ReservationPayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal amountRefunded = payments.stream()
				.filter(payment -> payment.getStatus() == ReservationPaymentStatus.REFUNDED)
				.map(ReservationPayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal outstandingBalance = totalStayAmount.subtract(amountPaid);
		if (amountRefunded.signum() > 0) {
			outstandingBalance = outstandingBalance.subtract(amountRefunded);
		}
		if (outstandingBalance.signum() < 0) {
			outstandingBalance = BigDecimal.ZERO;
		}

		List<ReservationFolioResponse.FolioLine> lines = new ArrayList<>();
		if (totalStayAmount.signum() > 0) {
			lines.add(new ReservationFolioResponse.FolioLine(
					"ROOM_CHARGE",
					"Room charge for stay",
					totalStayAmount,
					reservation.getCheckInDate()));
		}
		for (ReservationPayment payment : payments) {
			if (payment.getStatus() == ReservationPaymentStatus.AUTHORIZED) {
				lines.add(new ReservationFolioResponse.FolioLine(
						"AUTHORIZATION",
						"Payment authorization",
						payment.getAmount(),
						reservation.getCheckInDate()));
			}
			if (payment.getStatus() == ReservationPaymentStatus.CAPTURED) {
				lines.add(new ReservationFolioResponse.FolioLine(
						"CAPTURE",
						"Payment capture",
						payment.getAmount(),
						reservation.getCheckInDate()));
			}
			if (payment.getStatus() == ReservationPaymentStatus.REFUNDED) {
				lines.add(new ReservationFolioResponse.FolioLine(
						"REFUND",
						"Refund",
						payment.getAmount().negate(),
						reservation.getCheckInDate()));
			}
		}

		return new ReservationFolioResponse(
				reservation.getId(),
				reservation.getBookingReference(),
				hotelSettingsRepository.findById((short) 1)
						.map(HotelSettings::getCurrencyCode)
						.orElse("USD"),
				totalStayAmount,
				amountPaid,
				amountRefunded,
				outstandingBalance,
				lines);
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

	@Transactional(readOnly = true)
	public ReservationDashboardResponse getStaffDashboard(LocalDate date) {
		return getDashboard(date);
	}

	@Transactional(readOnly = true)
	public ReservationDashboardResponse getDashboard(LocalDate date) {
		LocalDate dashboardDate = date == null ? LocalDate.now() : date;
		List<Reservation> reservations = reservationRepository.findAll();
		Set<Long> occupiedRoomIds = roomAllocationRepository.findAll().stream()
				.filter(RoomAllocation::isActive)
				.filter(allocation -> !dashboardDate.isBefore(allocation.getCheckInDate())
						&& dashboardDate.isBefore(allocation.getCheckOutDate()))
				.map(RoomAllocation::getRoomId)
				.collect(Collectors.toSet());
		List<Reservation> arrivals = reservations.stream()
				.filter(reservation -> reservation.getCheckInDate().equals(dashboardDate))
				.filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED)
				.filter(reservation -> reservation.getStatus() != ReservationStatus.NO_SHOW)
				.toList();
		List<Reservation> checkedIn = reservations.stream()
				.filter(reservation -> reservation.getStatus() == ReservationStatus.CHECKED_IN)
				.filter(reservation -> !dashboardDate.isBefore(reservation.getCheckInDate())
						&& dashboardDate.isBefore(reservation.getCheckOutDate()))
				.toList();
		List<Reservation> upcoming = reservations.stream()
				.filter(reservation -> reservation.getCheckInDate().isAfter(dashboardDate.minusDays(1)))
				.filter(reservation -> reservation.getCheckInDate().isBefore(dashboardDate.plusDays(8)))
				.filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED)
				.filter(reservation -> reservation.getStatus() != ReservationStatus.NO_SHOW)
				.sorted(Comparator.comparing(Reservation::getCheckInDate))
				.limit(10)
				.toList();
		long availableRooms = roomRepository.findAll().stream()
				.filter(room -> room.getOperationalStatus() == OperationalStatus.AVAILABLE)
				.filter(room -> room.getHousekeepingStatus() == HousekeepingStatus.CLEAN
						|| room.getHousekeepingStatus() == HousekeepingStatus.INSPECTED)
				.filter(room -> !occupiedRoomIds.contains(room.getId()))
				.count();
		Page<ReservationResponse> upcomingPage = toResponsePage(new PageImpl<>(upcoming, Pageable.unpaged(), upcoming.size()));
		return new ReservationDashboardResponse(
				dashboardDate,
				arrivals.size(),
				checkedIn.size(),
				occupiedRoomIds.size(),
				availableRooms,
				upcomingPage.getContent());
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

	@Transactional
	public ReservationResponse confirmReservation(UUID reservationId) {
		return transitionReservationStatus(reservationId, ReservationStatus.CONFIRMED);
	}

	@Transactional
	public ReservationResponse cancelReservation(UUID reservationId) {
		return transitionReservationStatus(reservationId, ReservationStatus.CANCELLED);
	}

	@Transactional
	public ReservationResponse markNoShowReservation(UUID reservationId) {
		return transitionReservationStatus(reservationId, ReservationStatus.NO_SHOW);
	}

	@Transactional
	public ReservationResponse checkInReservation(UUID reservationId) {
		return transitionReservationStatus(reservationId, ReservationStatus.CHECKED_IN);
	}

	@Transactional
	public ReservationResponse checkOutReservation(UUID reservationId) {
		return transitionReservationStatus(reservationId, ReservationStatus.CHECKED_OUT);
	}

	private ReservationResponse transitionReservationStatus(UUID reservationId, ReservationStatus targetStatus) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException(reservationId));
		Instant now = Instant.now();
		reservation.transitionTo(targetStatus, now);
		if (targetStatus == ReservationStatus.CANCELLED
				|| targetStatus == ReservationStatus.NO_SHOW
				|| targetStatus == ReservationStatus.CHECKED_OUT) {
			roomAllocationRepository.findAllByReservation_IdAndActiveTrue(reservation.getId())
					.forEach(RoomAllocation::release);
		}
		return toResponsePage(new PageImpl<>(List.of(reservation))).getContent().getFirst();
	}

	private Reservation findReservation(UUID reservationId) {
		return reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException(reservationId));
	}

	private ReservationPayment createPayment(
			Reservation reservation,
			ReservationPaymentRequest request,
			ReservationPaymentStatus status) {
		if (request.amount() == null || request.amount().signum() <= 0) {
			throw new InvalidReservationRequestException("Payment amount must be greater than zero");
		}
		ReservationPayment payment = new ReservationPayment(
				reservation,
				request.provider(),
				request.providerReference(),
				status,
				request.amount(),
				request.currency());
		return reservationPaymentRepository.save(payment);
	}

	private ReservationPaymentResponse toPaymentResponse(ReservationPayment payment, UUID reservationId) {
		return new ReservationPaymentResponse(
				payment.getId(),
				reservationId,
				payment.getStatus(),
				payment.getAmount(),
				payment.getCurrency(),
				payment.getProvider(),
				payment.getProviderReference(),
				payment.getCreatedAt());
	}

	private void ensureReservationAcceptsPayment(Reservation reservation) {
		ReservationStatus status = reservation.getStatus();
		if (status == ReservationStatus.CANCELLED || status == ReservationStatus.NO_SHOW || status == ReservationStatus.CHECKED_OUT) {
			throw new IllegalStateException("Payment is no longer allowed for a completed reservation");
		}
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
