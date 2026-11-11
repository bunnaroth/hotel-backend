package com.example.hotel.module.guest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Email @Size(max = 254) String email,
		@Size(max = 40) String phoneNumber) {

	@AssertTrue(message = "At least one contact method is required")
	public boolean hasContactMethod() {
		return (email != null && !email.isBlank()) || (phoneNumber != null && !phoneNumber.isBlank());
	}
}
