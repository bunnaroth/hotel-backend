package com.example.hotel.module.reservation.repository;

import java.util.List;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.entity.ReservationGuestId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, ReservationGuestId> {

	List<ReservationGuest> findAllByReservation_Id(UUID reservationId);

	@EntityGraph(attributePaths = "guest")
	List<ReservationGuest> findAllByReservation_IdIn(List<UUID> reservationIds);
}
