package com.example.hotel.module.reservation.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

	List<Reservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
			ReservationStatus status,
			Instant now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from Reservation reservation where reservation.id = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

	@Query("""
			select reservation
			from Reservation reservation
			join fetch reservation.primaryGuest guest
			where lower(reservation.bookingReference) = lower(:bookingReference)
				and lower(guest.email) = lower(:email)
			""")
	Optional<Reservation> findForPublicLookup(
			@Param("bookingReference") String bookingReference,
			@Param("email") String email);
}
