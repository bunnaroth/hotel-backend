package com.example.hotel.module.room.repository;

import java.util.List;
import java.util.Optional;

import com.example.hotel.module.room.entity.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

	List<RoomType> findAllByOrderByCodeAsc();

	boolean existsByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select roomType from RoomType roomType where roomType.id = :id")
	Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
}
