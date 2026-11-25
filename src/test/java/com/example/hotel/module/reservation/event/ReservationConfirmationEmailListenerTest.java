package com.example.hotel.module.reservation.event;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReservationConfirmationEmailListenerTest {

	@Test
	void sendsReferenceAndConfirmationToPrimaryGuest() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		ReservationConfirmationEmailListener listener =
				new ReservationConfirmationEmailListener(mailSender, "bookings@example.test");

		listener.sendConfirmationEmail(new ReservationConfirmedEvent(
				"HTL-26-A8K92F",
				"Pat Guest",
				"pat@example.test"));

		var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		SimpleMailMessage message = messageCaptor.getValue();
		assertArrayEquals(new String[] { "pat@example.test" }, message.getTo());
		assertEquals(
				"Your Hotel Reservation is Confirmed – HTL-26-A8K92F",
				message.getSubject());
		assertTrue(message.getText().contains("Dear Pat Guest,"));
		assertTrue(message.getText().contains("Booking Reference: HTL-26-A8K92F"));
	}

	@Test
	void doesNotSendWhenGuestHasNoEmail() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		ReservationConfirmationEmailListener listener =
				new ReservationConfirmationEmailListener(mailSender, "bookings@example.test");

		listener.sendConfirmationEmail(new ReservationConfirmedEvent("HTL-26-A8K92F", "Pat Guest", null));

		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	void emailFailureDoesNotUndoConfirmedBooking() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		doThrow(new MailSendException("SMTP unavailable"))
				.when(mailSender).send(any(SimpleMailMessage.class));
		ReservationConfirmationEmailListener listener =
				new ReservationConfirmationEmailListener(mailSender, "bookings@example.test");

		assertDoesNotThrow(() -> listener.sendConfirmationEmail(
				new ReservationConfirmedEvent("HTL-26-A8K92F", "Pat Guest", "pat@example.test")));
	}
}
