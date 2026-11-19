package com.example.hotel.module.reservation.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.ReservationLookupChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationLookupChallengeRepository
		extends JpaRepository<ReservationLookupChallenge, UUID> {

	long countByLookupHashAndCreatedAtAfter(String lookupHash, Instant createdAt);

	long countByClientIpHashAndCreatedAtAfter(String clientIpHash, Instant createdAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select challenge from ReservationLookupChallenge challenge where challenge.id = :id")
	Optional<ReservationLookupChallenge> findByIdForUpdate(@Param("id") UUID id);

	@Modifying
	@Query("delete from ReservationLookupChallenge challenge where challenge.expiresAt < :cutoff")
	int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
