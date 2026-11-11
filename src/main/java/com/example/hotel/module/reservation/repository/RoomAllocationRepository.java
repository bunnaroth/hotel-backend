package com.example.hotel.module.reservation.repository;

import java.util.List;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.RoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, UUID> {

	List<RoomAllocation> findAllByReservation_Id(UUID reservationId);

	List<RoomAllocation> findAllByReservation_IdAndActiveTrue(UUID reservationId);

	List<RoomAllocation> findAllByReservation_IdIn(List<UUID> reservationIds);
}
