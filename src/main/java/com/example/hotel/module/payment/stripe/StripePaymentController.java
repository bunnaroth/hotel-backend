package com.example.hotel.module.payment.stripe;

import java.util.UUID;

import com.example.hotel.module.reservation.dto.StripeCheckoutSessionResponse;
import com.example.hotel.module.reservation.dto.StripeCheckoutStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stripe payments", description = "Stripe-hosted Checkout and webhook integration")
public class StripePaymentController {

	private final StripeCheckoutService checkoutService;

	public StripePaymentController(StripeCheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/api/reservations/{id}/payments/checkout")
	@Operation(
			summary = "Create a Stripe Checkout session",
			description = "Creates a hosted Stripe Checkout session for a pending reservation. The response includes the URL to which the guest should be redirected.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Checkout session created"),
			@ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
			@ApiResponse(responseCode = "502", description = "Stripe provider error", content = @Content),
			@ApiResponse(responseCode = "503", description = "Stripe is not configured", content = @Content)
	})
	public StripeCheckoutSessionResponse createCheckout(
			@Parameter(description = "Reservation UUID") @PathVariable UUID id) {
		return checkoutService.createCheckoutSession(id);
	}

	@GetMapping("/api/payments/stripe/checkout-sessions/{sessionId}")
	@Operation(
			summary = "Read a Checkout session status",
			description = "Returns reservation and payment state for a Checkout session. While payment is pending, the backend may reconcile the state with Stripe.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Reservation and payment status returned"),
			@ApiResponse(responseCode = "404", description = "Checkout session not found", content = @Content)
	})
	public StripeCheckoutStatusResponse checkoutStatus(
			@Parameter(description = "Stripe Checkout session ID") @PathVariable String sessionId) {
		return checkoutService.getCheckoutStatus(sessionId);
	}

	@PostMapping("/api/payments/stripe/webhook")
	@Operation(
			summary = "Receive Stripe webhook events",
			description = "Verifies the Stripe-Signature header and processes supported Checkout lifecycle events. Configure this URL in Stripe's webhook settings; browser redirects do not confirm payment.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Webhook accepted"),
			@ApiResponse(responseCode = "400", description = "Signature is invalid", content = @Content)
	})
	public ResponseEntity<Void> stripeWebhook(
			@RequestBody String payload,
			@Parameter(description = "Stripe webhook signature", required = true)
			@RequestHeader("Stripe-Signature") String signature) {
		checkoutService.handleWebhook(payload, signature);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
