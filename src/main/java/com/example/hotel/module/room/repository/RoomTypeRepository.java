package com.example.hotel.module.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.hotel.module.room.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
}
