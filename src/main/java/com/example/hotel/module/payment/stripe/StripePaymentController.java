package com.example.hotel.module.payment.stripe;

import java.util.UUID;

import com.example.hotel.module.reservation.dto.StripeCheckoutSessionResponse;
import com.example.hotel.module.reservation.dto.StripeCheckoutStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripePaymentController {

	private final StripeCheckoutService checkoutService;

	public StripePaymentController(StripeCheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/api/reservations/{id}/payments/checkout")
	public StripeCheckoutSessionResponse createCheckout(@PathVariable UUID id) {
		return checkoutService.createCheckoutSession(id);
	}

	@GetMapping("/api/payments/stripe/checkout-sessions/{sessionId}")
	public StripeCheckoutStatusResponse checkoutStatus(@PathVariable String sessionId) {
		return checkoutService.getCheckoutStatus(sessionId);
	}

	@PostMapping("/api/payments/stripe/webhook")
	public ResponseEntity<Void> stripeWebhook(
			@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String signature) {
		checkoutService.handleWebhook(payload, signature);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
