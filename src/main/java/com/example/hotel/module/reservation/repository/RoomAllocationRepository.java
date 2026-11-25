package com.example.hotel.module.reservation.repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import com.example.hotel.module.reservation.entity.RoomAllocation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, UUID> {

	List<RoomAllocation> findAllByReservation_Id(UUID reservationId);

	List<RoomAllocation> findAllByReservation_IdAndActiveTrue(UUID reservationId);

	boolean existsByRoom_Id(Long roomId);

	boolean existsByRoom_IdAndActiveTrue(Long roomId);

	boolean existsByRoom_RoomType_IdAndActiveTrue(Long roomTypeId);

	@Query("""
			select coalesce(sum(allocation.allocatedUnits), 0)
			from RoomAllocation allocation
			where allocation.room.id = :roomId
			  and allocation.active = true
			  and allocation.checkInDate < :checkOutDate
			  and allocation.checkOutDate > :checkInDate
			""")
	Long sumActiveUnitsForRoomAndDates(
			@Param("roomId") Long roomId,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate);

	@Query("""
			select allocation.room.id as roomId, sum(allocation.allocatedUnits) as allocatedUnits
			from RoomAllocation allocation
			where allocation.room.id in :roomIds
			  and allocation.active = true
			  and allocation.checkInDate < :checkOutDate
			  and allocation.checkOutDate > :checkInDate
			group by allocation.room.id
			""")
	List<AllocatedUnitCount> sumActiveUnitsForRoomsAndDates(
			@Param("roomIds") List<Long> roomIds,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate);

	interface AllocatedUnitCount {
		Long getRoomId();

		Long getAllocatedUnits();
	}
}
