package com.example.hotel.module.payment.stripe;

import java.time.Instant;
import java.util.Optional;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
public class StripeSdkGateway implements StripeGateway {

	@Override
	public CheckoutSession createCheckoutSession(CheckoutRequest request, String secretKey, String idempotencyKey) {
		SessionCreateParams.LineItem.PriceData.ProductData product =
				SessionCreateParams.LineItem.PriceData.ProductData.builder()
						.setName("Hotel reservation " + request.bookingReference())
						.setDescription("Stay from " + request.checkInDate() + " to " + request.checkOutDate())
						.build();
		SessionCreateParams.LineItem.PriceData price =
				SessionCreateParams.LineItem.PriceData.builder()
						.setCurrency(request.currency().toLowerCase())
						.setUnitAmount(request.amountMinor())
						.setProductData(product)
						.build();
		SessionCreateParams.LineItem lineItem =
				SessionCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(price)
						.build();

		SessionCreateParams.Builder params = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setSuccessUrl(request.successUrl())
				.setCancelUrl(request.cancelUrl())
				.setClientReferenceId(request.reservationId().toString())
				.setExpiresAt(request.expiresAt().getEpochSecond())
				.putMetadata("reservationId", request.reservationId().toString())
				.addLineItem(lineItem);
		if (request.guestEmail() != null && !request.guestEmail().isBlank()) {
			params.setCustomerEmail(request.guestEmail());
		}

		try {
			Session session = Session.create(
					params.build(),
					requestOptions(secretKey, idempotencyKey));
			if (session.getUrl() == null || session.getExpiresAt() == null) {
				throw new StripeGatewayException("Stripe returned an incomplete checkout session");
			}
			return new CheckoutSession(
					session.getId(),
					session.getUrl(),
					Instant.ofEpochSecond(session.getExpiresAt()));
		} catch (StripeException exception) {
			throw new StripeGatewayException("Stripe could not create the checkout session", exception);
		}
	}

	@Override
	public WebhookEvent verifyWebhook(String payload, String signature, String webhookSecret) {
		final Event event;
		try {
			event = Webhook.constructEvent(payload, signature, webhookSecret);
		} catch (SignatureVerificationException exception) {
			throw new StripeWebhookSignatureException("Stripe webhook signature is invalid", exception);
		} catch (RuntimeException exception) {
			throw new StripeGatewayException("Stripe webhook payload could not be parsed", exception);
		}

		if (!event.getType().startsWith("checkout.session.")) {
			return new WebhookEvent(event.getId(), event.getType(), null, null, null);
		}

		Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
		if (object.isEmpty() || !(object.get() instanceof Session session)) {
			throw new StripeGatewayException("Stripe checkout webhook did not contain a checkout session");
		}
		return new WebhookEvent(
				event.getId(),
				event.getType(),
				session.getId(),
				session.getPaymentStatus(),
				session.getPaymentIntent());
	}

	@Override
	public void refundPaymentIntent(String paymentIntentId, long amountMinor, String secretKey, String idempotencyKey) {
		RefundCreateParams params = RefundCreateParams.builder()
				.setPaymentIntent(paymentIntentId)
				.setAmount(amountMinor)
				.build();
		try {
			Refund refund = Refund.create(params, requestOptions(secretKey, idempotencyKey));
			if (!"succeeded".equals(refund.getStatus())) {
				throw new StripeGatewayException("Stripe has not completed the refund");
			}
		} catch (StripeException exception) {
			throw new StripeGatewayException("Stripe could not refund the payment", exception);
		}
	}

	private static RequestOptions requestOptions(String secretKey, String idempotencyKey) {
		return RequestOptions.builder()
				.setApiKey(secretKey)
				.setIdempotencyKey(idempotencyKey)
				.build();
	}
}
