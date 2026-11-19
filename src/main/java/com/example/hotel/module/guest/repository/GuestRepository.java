package com.example.hotel.module.guest.repository;

import java.util.UUID;

import com.example.hotel.module.guest.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {
}
