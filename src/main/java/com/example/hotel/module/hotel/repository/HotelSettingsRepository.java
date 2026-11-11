package com.example.hotel.module.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.hotel.module.hotel.entity.HotelSettings;

public interface HotelSettingsRepository extends JpaRepository<HotelSettings, Short> {
}
