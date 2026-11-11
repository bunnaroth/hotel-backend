package com.example.hotel.module.auth.service;

import java.util.Locale;
import java.util.Set;

import com.example.hotel.module.auth.entity.RoleEntity;
import com.example.hotel.module.auth.entity.UserAccount;
import com.example.hotel.module.auth.repository.RoleRepository;
import com.example.hotel.module.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final String loginName;
	private final String password;

	public BootstrapAdminInitializer(
			UserAccountRepository userAccountRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.security.bootstrap-admin.login}") String loginName,
			@Value("${app.security.bootstrap-admin.password}") String password) {
		this.userAccountRepository = userAccountRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginName = loginName;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (loginName.isBlank() && password.isBlank()) {
			return;
		}
		if (loginName.isBlank() || password.isBlank()) {
			throw new IllegalStateException("INITIAL_ADMIN_LOGIN and INITIAL_ADMIN_PASSWORD must be set together");
		}
		if (password.length() < 12 || !AuthService.passwordWithinBcryptLimit(password)) {
			throw new IllegalStateException("Initial administrator password must be 12 to 72 UTF-8 bytes");
		}
		if (userAccountRepository.count() != 0) {
			return;
		}
		if (loginName.trim().length() > 254) {
			throw new IllegalStateException("Initial administrator login must be at most 254 characters");
		}
		RoleEntity ownerRole = roleRepository.findByRoleName("OWNER")
				.orElseThrow(() -> new IllegalStateException("Required OWNER role was not seeded by Flyway"));
		UserAccount owner = new UserAccount(
				loginName.trim().toLowerCase(Locale.ROOT),
				passwordEncoder.encode(password),
				Set.of(ownerRole));
		userAccountRepository.save(owner);
	}
}
