package com.example.hotel.module.reservation.repository;

import com.example.hotel.module.reservation.entity.ReservationGuest;
import com.example.hotel.module.reservation.entity.ReservationGuestId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, ReservationGuestId> {
}
