package com.example.hotel.module.auth.dto;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt) {

	@Override
	public String toString() {
		return "LoginResponse[tokenType=" + tokenType + ", expiresAt=" + expiresAt + "]";
	}
}
