package com.example.hotel.module.reservation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.ReservationPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, UUID> {

	List<ReservationPayment> findAllByReservation_Id(UUID reservationId);

	List<ReservationPayment> findAllByReservation_IdOrderByCreatedAtAsc(UUID reservationId);

	Optional<ReservationPayment> findFirstByReservation_IdOrderByCreatedAtDesc(UUID reservationId);

	Optional<ReservationPayment> findFirstByReservation_IdAndProviderOrderByCreatedAtDesc(
			UUID reservationId,
			String provider);

	Optional<ReservationPayment> findByCheckoutSessionId(String checkoutSessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select payment from ReservationPayment payment where payment.checkoutSessionId = :sessionId")
	Optional<ReservationPayment> findByCheckoutSessionIdForUpdate(@Param("sessionId") String checkoutSessionId);
}
