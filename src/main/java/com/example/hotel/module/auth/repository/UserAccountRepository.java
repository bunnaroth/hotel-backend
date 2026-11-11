package com.example.hotel.module.auth.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.hotel.module.auth.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select account from UserAccount account where account.loginName = :loginName")
	Optional<UserAccount> findForAuthentication(@Param("loginName") String loginName);
}
