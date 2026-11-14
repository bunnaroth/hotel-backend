package com.example.hotel.module.guest.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.hotel.module.guest.dto.GuestReservationHistoryResponse;
import com.example.hotel.module.guest.dto.GuestResponse;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.guest.repository.GuestRepository;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.repository.ReservationGuestRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestService {

	private final GuestRepository guestRepository;
	private final ReservationGuestRepository reservationGuestRepository;

	public GuestService(GuestRepository guestRepository, ReservationGuestRepository reservationGuestRepository) {
		this.guestRepository = guestRepository;
		this.reservationGuestRepository = reservationGuestRepository;
	}

	@Transactional(readOnly = true)
	public Page<GuestResponse> search(String query, Pageable pageable) {
		String normalizedQuery = query == null ? "" : query.trim();
		if (normalizedQuery.length() < 2) {
			throw new InvalidGuestSearchException();
		}

		String pattern = "%" + escapeLike(normalizedQuery.toLowerCase(Locale.ROOT)) + "%";
		Specification<Guest> specification = (root, criteriaQuery, criteriaBuilder) -> {
			Predicate firstName = criteriaBuilder.like(
					criteriaBuilder.lower(root.get("firstName")), pattern, '!');
			Predicate lastName = criteriaBuilder.like(
					criteriaBuilder.lower(root.get("lastName")), pattern, '!');
			Predicate email = criteriaBuilder.like(
					criteriaBuilder.lower(root.get("email")), pattern, '!');
			Predicate phoneNumber = criteriaBuilder.like(root.get("phoneNumber"), pattern, '!');
			return criteriaBuilder.or(firstName, lastName, email, phoneNumber);
		};

		return guestRepository.findAll(specification, pageable).map(GuestService::toResponse);
	}

	@Transactional(readOnly = true)
	public List<GuestReservationHistoryResponse> getGuestReservationHistory(UUID guestId) {
		if (guestId == null) {
			throw new InvalidGuestSearchException();
		}
		List<UUID> reservationIds = reservationGuestRepository.findAllByGuest_IdOrderByReservation_CheckInDateDesc(guestId)
				.stream()
				.map(ReservationGuest::getReservationId)
				.distinct()
				.toList();
		Map<UUID, List<ReservationGuest>> reservationsById = reservationGuestRepository.findAllByReservation_IdIn(reservationIds)
				.stream()
				.collect(Collectors.groupingBy(ReservationGuest::getReservationId));
		return reservationIds.stream()
				.map(reservationId -> toHistoryResponse(reservationsById.getOrDefault(reservationId, List.of())))
				.sorted(Comparator.comparing(GuestReservationHistoryResponse::checkInDate).reversed())
				.toList();
	}

	@Transactional(readOnly = true)
	public List<GuestReservationHistoryResponse> getReservationHistory(UUID guestId) {
		return getGuestReservationHistory(guestId);
	}

	@Transactional(readOnly = true)
	public List<GuestReservationHistoryResponse> getGuestHistory(UUID guestId) {
		return getGuestReservationHistory(guestId);
	}

	private GuestReservationHistoryResponse toHistoryResponse(List<ReservationGuest> reservationGuests) {
		ReservationGuest firstAssignment = reservationGuests.getFirst();
		Reservation reservation = firstAssignment.getReservation();
		return new GuestReservationHistoryResponse(
				reservation.getId(),
				reservation.getBookingReference(),
				reservation.getCheckInDate(),
				reservation.getCheckOutDate(),
				reservation.getStatus(),
				reservationGuests.stream()
						.map(link -> toResponse(link.getGuest()))
						.toList());
	}

	private static String escapeLike(String value) {
		return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

	private static GuestResponse toResponse(Guest guest) {
		return new GuestResponse(
				guest.getId(),
				guest.getFirstName(),
				guest.getLastName(),
				guest.getEmail(),
				guest.getPhoneNumber());
	}

	public static class InvalidGuestSearchException extends RuntimeException {

		public InvalidGuestSearchException() {
			super("Guest search query must contain at least 2 characters");
		}
	}
}
