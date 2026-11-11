package com.example.hotel.module.auth.controller;

import java.util.Map;

import com.example.hotel.module.auth.dto.LoginRequest;
import com.example.hotel.module.auth.dto.LoginResponse;
import com.example.hotel.module.auth.service.AuthService;
import com.example.hotel.module.auth.service.InvalidCredentialsException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.loginName(), request.password());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCredentials() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "Invalid login credentials"));
	}
}
