package com.example.hotel.module.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.hotel.module.auth.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByRoleName(String roleName);
}
