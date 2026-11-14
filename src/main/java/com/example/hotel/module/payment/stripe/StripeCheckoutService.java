package com.example.hotel.module.payment.stripe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.hotel.module.hotel.entity.HotelSettings;
import com.example.hotel.module.hotel.repository.HotelSettingsRepository;
import com.example.hotel.module.payment.stripe.StripeGateway.CheckoutRequest;
import com.example.hotel.module.payment.stripe.StripeGateway.CheckoutSession;
import com.example.hotel.module.payment.stripe.StripeGateway.WebhookEvent;
import com.example.hotel.module.reservation.dto.StripeCheckoutSessionResponse;
import com.example.hotel.module.reservation.dto.StripeCheckoutStatusResponse;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationPayment;
import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationPaymentRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.service.ReservationPricingService;
import com.example.hotel.module.reservation.service.ReservationService.ReservationNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeCheckoutService {

	private static final String STRIPE_PROVIDER = "STRIPE";
	private static final Duration CHECKOUT_SESSION_LIFETIME = Duration.ofMinutes(35);
	private static final Duration WEBHOOK_DELIVERY_GRACE = Duration.ofMinutes(10);
	private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
			"BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
			"MGA", "PYG", "RWF", "UGX", "VND", "VUV",
			"XAF", "XOF", "XPF");
	private static final Set<String> THREE_DECIMAL_CURRENCIES =
			Set.of("BHD", "JOD", "KWD", "OMR", "TND");

	private final ReservationRepository reservationRepository;
	private final ReservationPaymentRepository paymentRepository;
	private final HotelSettingsRepository hotelSettingsRepository;
	private final ReservationPricingService pricingService;
	private final StripeGateway stripeGateway;
	private final String secretKey;
	private final String webhookSecret;
	private final String successUrl;
	private final String cancelUrl;

	public StripeCheckoutService(
			ReservationRepository reservationRepository,
			ReservationPaymentRepository paymentRepository,
			HotelSettingsRepository hotelSettingsRepository,
			ReservationPricingService pricingService,
			StripeGateway stripeGateway,
			@Value("${app.payment.stripe.secret-key:}") String secretKey,
			@Value("${app.payment.stripe.webhook-secret:}") String webhookSecret,
			@Value("${app.payment.stripe.success-url}") String successUrl,
			@Value("${app.payment.stripe.cancel-url}") String cancelUrl) {
		this.reservationRepository = reservationRepository;
		this.paymentRepository = paymentRepository;
		this.hotelSettingsRepository = hotelSettingsRepository;
		this.pricingService = pricingService;
		this.stripeGateway = stripeGateway;
		this.secretKey = secretKey;
		this.webhookSecret = webhookSecret;
		this.successUrl = successUrl;
		this.cancelUrl = cancelUrl;
	}

	@Transactional(noRollbackFor = StripePaymentProviderException.class)
	public StripeCheckoutSessionResponse createCheckoutSession(UUID reservationId) {
		requireSecretKey();
		if (!successUrl.contains("{CHECKOUT_SESSION_ID}")) {
			throw new StripePaymentNotConfiguredException(
					"STRIPE_SUCCESS_URL must contain {CHECKOUT_SESSION_ID}");
		}

		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException(reservationId));
		Instant now = Instant.now();
		if (reservation.getStatus() != ReservationStatus.PENDING || reservation.isPendingAndExpired(now)) {
			throw new IllegalStateException("Only active pending reservations can start checkout");
		}

		Optional<ReservationPayment> latestStripePayment =
				paymentRepository.findFirstByReservation_IdAndProviderOrderByCreatedAtDesc(
						reservationId, STRIPE_PROVIDER);
		if (latestStripePayment.isPresent()) {
			ReservationPayment previous = latestStripePayment.get();
			if (previous.getStatus() == ReservationPaymentStatus.PENDING
					&& previous.getCheckoutUrl() != null
					&& previous.getCheckoutExpiresAt() != null
					&& previous.getCheckoutExpiresAt().isAfter(now)) {
				return toCheckoutResponse(reservation, previous);
			}
		}

		String currency = hotelSettingsRepository.findById((short) 1)
				.map(HotelSettings::getCurrencyCode)
				.orElse("USD")
				.toUpperCase(Locale.ROOT);
		BigDecimal amount = pricingService.calculateStayAmount(reservation);
		if (amount.signum() <= 0) {
			throw new IllegalStateException("Reservation has no payable room charges");
		}
		long amountMinor = toMinorUnits(amount, currency);
		Instant checkoutExpiresAt = now.plus(CHECKOUT_SESSION_LIFETIME);
		Instant reservationExpiresAt = checkoutExpiresAt.plus(WEBHOOK_DELIVERY_GRACE);
		Instant previousReservationExpiry = reservation.getExpiresAt();
		reservation.updatePendingExpiry(reservationExpiresAt);

		ReservationPayment payment = new ReservationPayment(
				reservation,
				STRIPE_PROVIDER,
				null,
				ReservationPaymentStatus.PENDING,
				amount,
				currency);
		payment.setCheckoutSession(null, null, checkoutExpiresAt);
		paymentRepository.saveAndFlush(payment);

		CheckoutRequest request = new CheckoutRequest(
				reservation.getId(),
				reservation.getBookingReference(),
				reservation.getCheckInDate(),
				reservation.getCheckOutDate(),
				reservation.getPrimaryGuest().getEmail(),
				amount,
				amountMinor,
				currency,
				checkoutExpiresAt,
				successUrl,
				cancelUrl);
		try {
			CheckoutSession session = stripeGateway.createCheckoutSession(
					request,
					secretKey,
					"reservation-checkout-" + payment.getId());
			payment.setCheckoutSession(session.id(), session.url(), session.expiresAt());
			paymentRepository.save(payment);
			return toCheckoutResponse(reservation, payment);
		} catch (StripeGatewayException exception) {
			payment.setStatus(ReservationPaymentStatus.FAILED);
			paymentRepository.save(payment);
			reservation.updatePendingExpiry(previousReservationExpiry);
			throw new StripePaymentProviderException("Unable to start Stripe Checkout", exception);
		}
	}

	@Transactional(readOnly = true)
	public StripeCheckoutStatusResponse getCheckoutStatus(String sessionId) {
		ReservationPayment payment = paymentRepository.findByCheckoutSessionId(sessionId)
				.orElseThrow(() -> new StripeCheckoutNotFoundException(sessionId));
		Reservation reservation = payment.getReservation();
		return new StripeCheckoutStatusResponse(
				reservation.getId(),
				reservation.getBookingReference(),
				reservation.getStatus(),
				payment.getStatus());
	}

	@Transactional
	public void handleWebhook(String payload, String signature) {
		if (webhookSecret == null || webhookSecret.isBlank()) {
			throw new StripePaymentNotConfiguredException("Stripe webhook signing secret is not configured");
		}
		WebhookEvent event = stripeGateway.verifyWebhook(payload, signature, webhookSecret);
		switch (event.type()) {
			case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
					markPaymentSuccessful(event);
			case "checkout.session.expired", "checkout.session.async_payment_failed" ->
					markPaymentFailed(event);
			default -> {
			}
		}
	}

	private void markPaymentSuccessful(WebhookEvent event) {
		if (!"paid".equals(event.paymentStatus())) {
			return;
		}
		ReservationPayment payment = findPaymentForUpdate(event.sessionId());
		if (payment.getStatus() == ReservationPaymentStatus.CAPTURED
				|| payment.getStatus() == ReservationPaymentStatus.REFUNDED) {
			return;
		}
		if (event.paymentIntentId() == null || event.paymentIntentId().isBlank()) {
			throw new StripeGatewayException("Paid Stripe checkout has no payment intent");
		}

		Reservation reservation = lockReservation(payment);
		payment.setProviderReference(event.paymentIntentId());
		payment.setStatus(ReservationPaymentStatus.CAPTURED);
		if (reservation.getStatus() == ReservationStatus.PENDING) {
			reservation.transitionTo(ReservationStatus.CONFIRMED, Instant.now());
		} else if (reservation.getStatus() == ReservationStatus.CANCELLED
				|| reservation.getStatus() == ReservationStatus.NO_SHOW) {
			requireSecretKey();
			stripeGateway.refundPaymentIntent(
					event.paymentIntentId(),
					toMinorUnits(payment.getAmount(), payment.getCurrency()),
					secretKey,
					"late-checkout-refund-" + event.sessionId());
			payment.setStatus(ReservationPaymentStatus.REFUNDED);
		}
		paymentRepository.save(payment);
		reservationRepository.save(reservation);
	}

	private void markPaymentFailed(WebhookEvent event) {
		ReservationPayment payment = findPaymentForUpdate(event.sessionId());
		if (payment.getStatus() == ReservationPaymentStatus.PENDING) {
			payment.setStatus(ReservationPaymentStatus.FAILED);
			paymentRepository.save(payment);
		}
	}

	private ReservationPayment findPaymentForUpdate(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new StripeGatewayException("Stripe checkout webhook has no session id");
		}
		ReservationPayment payment = paymentRepository.findByCheckoutSessionId(sessionId)
				.orElseThrow(() -> new StripeGatewayException("Stripe checkout session is not registered"));
		lockReservation(payment);
		return paymentRepository.findByCheckoutSessionIdForUpdate(sessionId)
				.orElseThrow(() -> new StripeGatewayException("Stripe checkout session is not registered"));
	}

	private Reservation lockReservation(ReservationPayment payment) {
		UUID reservationId = payment.getReservation().getId();
		return reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException(reservationId));
	}

	private void requireSecretKey() {
		if (secretKey == null || secretKey.isBlank()) {
			throw new StripePaymentNotConfiguredException("Stripe secret key is not configured");
		}
	}

	private static long toMinorUnits(BigDecimal amount, String currency) {
		int fractionDigits = switch (currency.toUpperCase(Locale.ROOT)) {
			case "CLF" -> 4;
			default -> ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT)) ? 0
					: THREE_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT)) ? 3 : 2;
		};
		try {
			return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY)
					.movePointRight(fractionDigits)
					.longValueExact();
		} catch (ArithmeticException exception) {
			throw new IllegalStateException(
					"Reservation total cannot be represented in the minor units for " + currency,
					exception);
		}
	}

	private static StripeCheckoutSessionResponse toCheckoutResponse(
			Reservation reservation,
			ReservationPayment payment) {
		return new StripeCheckoutSessionResponse(
				reservation.getId(),
				reservation.getBookingReference(),
				payment.getCheckoutSessionId(),
				payment.getCheckoutUrl(),
				payment.getAmount(),
				payment.getCurrency(),
				payment.getCheckoutExpiresAt());
	}
}
