package com.example.hotel.module.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.example.hotel.module.auth.dto.LoginResponse;
import com.example.hotel.module.auth.entity.UserAccount;
import com.example.hotel.module.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final String ISSUER = "hmos-api";
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final int maxFailedAttempts;
	private final Duration lockDuration;
	private final Duration accessTokenTtl;
	private final String dummyPasswordHash;

	public AuthService(
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			JwtEncoder jwtEncoder,
			@Value("${app.security.login.max-failed-attempts}") int maxFailedAttempts,
			@Value("${app.security.login.lock-duration}") Duration lockDuration,
			@Value("${app.security.jwt.access-token-ttl}") Duration accessTokenTtl) {
		if (maxFailedAttempts < 1 || lockDuration.isNegative() || lockDuration.isZero()
				|| accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
			throw new IllegalArgumentException("Authentication durations and attempt limits must be positive");
		}
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.maxFailedAttempts = maxFailedAttempts;
		this.lockDuration = lockDuration;
		this.accessTokenTtl = accessTokenTtl;
		this.dummyPasswordHash = passwordEncoder.encode("invalid-user-" + Instant.now());
	}

	@Transactional(noRollbackFor = InvalidCredentialsException.class)
	public LoginResponse login(String suppliedLoginName, String password) {
		String loginName = suppliedLoginName.trim().toLowerCase(Locale.ROOT);
		UserAccount account = userAccountRepository.findForAuthentication(loginName).orElse(null);
		if (account == null) {
			String candidate = passwordWithinBcryptLimit(password) ? password : "invalid-password";
			passwordEncoder.matches(candidate, dummyPasswordHash);
			throw new InvalidCredentialsException();
		}

		boolean withinBcryptLimit = passwordWithinBcryptLimit(password);
		String candidate = withinBcryptLimit ? password : "invalid-password";
		boolean encodedPasswordMatches = passwordEncoder.matches(candidate, account.getPasswordHash());
		boolean passwordMatches = withinBcryptLimit && encodedPasswordMatches;
		Instant now = Instant.now();
		if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
			throw new InvalidCredentialsException();
		}
		if (account.getLockedUntil() != null) {
			account.resetFailedLogins();
		}
		if (!passwordMatches) {
			account.recordFailedLogin(maxFailedAttempts, now.plus(lockDuration));
			throw new InvalidCredentialsException();
		}
		if (!account.isActive() || account.getRoles().isEmpty()) {
			throw new InvalidCredentialsException();
		}

		account.resetFailedLogins();
		Instant expiresAt = now.plus(accessTokenTtl);
		List<String> roles = account.getRoles().stream()
				.map(role -> role.getRoleName())
				.sorted()
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(account.getLoginName())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("roles", roles)
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new LoginResponse(token, "Bearer", expiresAt);
	}

	public static boolean passwordWithinBcryptLimit(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length <= 72;
	}
}
