package com.example.hotel.module.guest.service;

import com.example.hotel.module.guest.dto.GuestResponse;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.guest.repository.GuestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.Locale;

@Service
public class GuestService {

	private final GuestRepository guestRepository;

	public GuestService(GuestRepository guestRepository) {
		this.guestRepository = guestRepository;
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
