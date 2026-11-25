package com.example.hotel.module.room.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.enums.RoomCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<HotelRoom, Long> {

	List<HotelRoom> findAllByOrderByRoomNumberAsc();

	boolean existsByRoomNumberIgnoreCase(String roomNumber);

	boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber, Long id);

	long countByRoomType_Id(Long roomTypeId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select room from HotelRoom room where room.roomType.id = :roomTypeId order by room.id")
	List<HotelRoom> findAllByRoomTypeIdForUpdate(@Param("roomTypeId") Long roomTypeId);

	@Query("""
			select room from HotelRoom room join fetch room.roomType
			where room.id in :roomIds
			  and room.operationalStatus = :operationalStatus
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
			@Param("operationalStatus") OperationalStatus operationalStatus);

	@Query("""
			select room from HotelRoom room join fetch room.roomType
			where room.operationalStatus = :operationalStatus
			  and room.roomType.active = true
			  and (:roomTypeId is null or room.roomType.id = :roomTypeId)
			  and (
			      (room.roomType.category = :dormitoryCategory
			       and room.roomType.maxOccupancy > coalesce((
			           select sum(allocation.allocatedUnits) from RoomAllocation allocation
			           where allocation.room = room
			             and allocation.active = true
			             and allocation.checkInDate < :checkOutDate
			             and allocation.checkOutDate > :checkInDate
			       ), 0))
			      or
			      (room.roomType.category <> :dormitoryCategory
			       and room.roomType.maxOccupancy >= :guestCount
			       and not exists (
			           select allocation.id from RoomAllocation allocation
			           where allocation.room = room
			             and allocation.active = true
			             and allocation.checkInDate < :checkOutDate
			             and allocation.checkOutDate > :checkInDate
			       ))
			  )
			""")
	Page<HotelRoom> findAvailableRooms(
			@Param("roomTypeId") Long roomTypeId,
			@Param("guestCount") int guestCount,
			@Param("checkInDate") LocalDate checkInDate,
			@Param("checkOutDate") LocalDate checkOutDate,
			@Param("dormitoryCategory") RoomCategory dormitoryCategory,
			@Param("operationalStatus") OperationalStatus operationalStatus,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select room from HotelRoom room join fetch room.roomType where room.id = :roomId")
	Optional<HotelRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
