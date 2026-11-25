package com.example.hotel.module.guest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "GuestRequest", description = "Guest contact details. At least one of email or phoneNumber is required.")
public record GuestRequest(
		@Schema(description = "Guest's first name.", example = "Pat")
		@NotBlank @Size(max = 100) String firstName,
		@Schema(description = "Guest's last name.", example = "Guest")
		@NotBlank @Size(max = 100) String lastName,
		@Schema(description = "Email address used for booking lookup and notifications when provided.", example = "pat@example.test")
		@Email @Size(max = 254) String email,
		@Schema(description = "Optional contact phone number.", example = "+1-555-0100")
		@Size(max = 40) String phoneNumber) {

	@AssertTrue(message = "At least one contact method is required")
	public boolean hasContactMethod() {
		return (email != null && !email.isBlank()) || (phoneNumber != null && !phoneNumber.isBlank());
	}
}
