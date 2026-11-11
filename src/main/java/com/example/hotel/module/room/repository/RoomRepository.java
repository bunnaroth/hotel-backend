package com.example.hotel.module.room.repository;

import java.time.LocalDate;
import java.util.List;

import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<HotelRoom, Long> {

	@Query("""
			select room from HotelRoom room
			where (:roomTypeId is null or room.roomType.id = :roomTypeId)
			  and (:operationalStatus is null or room.operationalStatus = :operationalStatus)
			  and (:housekeepingStatus is null or room.housekeepingStatus = :housekeepingStatus)
			""")
	Page<HotelRoom> search(
			@Param("roomTypeId") Long roomTypeId,
			@Param("operationalStatus") OperationalStatus operationalStatus,
			@Param("housekeepingStatus") HousekeepingStatus housekeepingStatus,
			Pageable pageable);

	@Query("""
			select room from HotelRoom room join fetch room.roomType
			where room.id in :roomIds
			  and room.operationalStatus = :operationalStatus
			  and room.housekeepingStatus in :housekeepingStatuses
			  and room.roomType.active = true
			  and not exists (
			      select allocation.id from RoomAllocation allocation
			      where allocation.room = room
			        and allocation.active = true
			        and allocation.checkInDate < :checkOutDate
			        and allocation.checkOutDate > :checkInDate
			  )
			""")
	List<HotelRoom> findAvailableRooms(
			@Param("roomIds") List<Long> roomIds,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("operationalStatus") OperationalStatus operationalStatus,
			@Param("housekeepingStatuses") List<HousekeepingStatus> housekeepingStatuses);

	@Query("""
			select room from HotelRoom room join fetch room.roomType
			where room.operationalStatus = :operationalStatus
			  and room.housekeepingStatus in :housekeepingStatuses
			  and room.roomType.active = true
			  and (:roomTypeId is null or room.roomType.id = :roomTypeId)
			  and room.roomType.maxOccupancy >= :guestCount
			  and not exists (
			      select allocation.id from RoomAllocation allocation
			      where allocation.room = room
			        and allocation.active = true
			        and allocation.checkInDate < :checkOutDate
			        and allocation.checkOutDate > :checkInDate
			  )
			""")
	Page<HotelRoom> findAvailableRooms(
			@Param("roomTypeId") Long roomTypeId,
			@Param("guestCount") int guestCount,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("operationalStatus") OperationalStatus operationalStatus,
			@Param("housekeepingStatuses") List<HousekeepingStatus> housekeepingStatuses,
			Pageable pageable);
}
