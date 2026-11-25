package com.example.hotel.module.payment.stripe;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.example.hotel.module.hotel.repository.HotelSettingsRepository;
import com.example.hotel.module.payment.stripe.StripeGateway.CheckoutSessionStatus;
import com.example.hotel.module.guest.entity.Guest;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationPayment;
import com.example.hotel.module.reservation.event.ReservationConfirmedEvent;
import com.example.hotel.module.reservation.enums.ReservationPaymentStatus;
import com.example.hotel.module.reservation.enums.ReservationStatus;
import com.example.hotel.module.reservation.repository.ReservationPaymentRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.service.ReservationPricingService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripeCheckoutStatusReconciliationTest {

	@Test
	void paidStripeSessionConfirmsReservationWhenWebhookHasNotArrived() {
		String sessionId = "cs_test_123";
		String secretKey = "sk_test_key";
		UUID reservationId = UUID.randomUUID();
		AtomicReference<ReservationPaymentStatus> paymentStatus =
				new AtomicReference<>(ReservationPaymentStatus.PENDING);
		AtomicReference<ReservationStatus> reservationStatus =
				new AtomicReference<>(ReservationStatus.PENDING);

		ReservationPaymentRepository paymentRepository = mock(ReservationPaymentRepository.class);
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		ReservationPricingService pricingService = mock(ReservationPricingService.class);
		HotelSettingsRepository hotelSettingsRepository = mock(HotelSettingsRepository.class);
		StripeGateway stripeGateway = mock(StripeGateway.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		ReservationPayment payment = mock(ReservationPayment.class);
		Reservation reservation = mock(Reservation.class);
		Guest primaryGuest = mock(Guest.class);

		when(paymentRepository.findByCheckoutSessionId(sessionId)).thenReturn(Optional.of(payment));
		when(paymentRepository.findByCheckoutSessionIdForUpdate(sessionId)).thenReturn(Optional.of(payment));
		when(payment.getStatus()).thenAnswer(invocation -> paymentStatus.get());
		when(payment.getReservation()).thenReturn(reservation);
		doAnswer(invocation -> {
			paymentStatus.set(invocation.getArgument(0, ReservationPaymentStatus.class));
			return null;
		}).when(payment).setStatus(any(ReservationPaymentStatus.class));

		when(reservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
		when(reservation.getId()).thenReturn(reservationId);
		when(reservation.getBookingReference()).thenReturn("HTL-26-A8K92F");
		when(reservation.getPrimaryGuest()).thenReturn(primaryGuest);
		when(primaryGuest.getFullName()).thenReturn("Pat Guest");
		when(primaryGuest.getEmail()).thenReturn("pat@example.test");
		when(reservation.getStatus()).thenAnswer(invocation -> reservationStatus.get());
		doAnswer(invocation -> {
			reservationStatus.set(invocation.getArgument(0, ReservationStatus.class));
			return null;
		}).when(reservation).transitionTo(any(ReservationStatus.class), any(Instant.class));

		when(stripeGateway.retrieveCheckoutSession(sessionId, secretKey)).thenReturn(
				new CheckoutSessionStatus("complete", "paid", "pi_test_123"));

		StripeCheckoutService service = new StripeCheckoutService(
				reservationRepository,
				paymentRepository,
				hotelSettingsRepository,
				pricingService,
				stripeGateway,
				eventPublisher,
				secretKey,
				"whsec_test",
				"http://localhost:3000/payment/success?session_id={CHECKOUT_SESSION_ID}",
				"http://localhost:3000/payment/cancelled");

		var response = service.getCheckoutStatus(sessionId);

		assertEquals(ReservationPaymentStatus.CAPTURED, response.paymentStatus());
		assertEquals(ReservationStatus.CONFIRMED, response.reservationStatus());
		var repeatedResponse = service.getCheckoutStatus(sessionId);
		assertEquals(ReservationPaymentStatus.CAPTURED, repeatedResponse.paymentStatus());
		verify(stripeGateway).retrieveCheckoutSession(sessionId, secretKey);
		verify(eventPublisher, times(1)).publishEvent(any(ReservationConfirmedEvent.class));
	}
}
