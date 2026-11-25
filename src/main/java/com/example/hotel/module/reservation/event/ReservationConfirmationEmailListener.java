package com.example.hotel.module.reservation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationConfirmationEmailListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReservationConfirmationEmailListener.class);

	private final JavaMailSender mailSender;
	private final String senderAddress;

	public ReservationConfirmationEmailListener(
			JavaMailSender mailSender,
			@Value("${app.mail.from:no-reply@example.com}") String senderAddress) {
		this.mailSender = mailSender;
		this.senderAddress = senderAddress;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendConfirmationEmail(ReservationConfirmedEvent event) {
		if (event.guestEmail() == null || event.guestEmail().isBlank()) {
			LOGGER.warn("No guest email is available for reservation confirmation ({})",
					event.bookingReference());
			return;
		}
		if (senderAddress == null || senderAddress.isBlank()) {
			LOGGER.error("Cannot send reservation confirmation email for {}: sender address is not configured",
					event.bookingReference());
			return;
		}

		String greeting = event.guestName() == null || event.guestName().isBlank()
				? "Dear Customer,"
				: "Dear " + event.guestName().trim() + ",";
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(senderAddress);
		message.setTo(event.guestEmail());
		message.setSubject("Your Hotel Reservation is Confirmed – " + event.bookingReference());
		message.setText("""
				%s

				Thank you for booking with us. Your reservation has been successfully confirmed.

				Booking Reference: %s

				Please keep this reference for future communication with our hotel.

				We look forward to welcoming you!
				""".formatted(greeting, event.bookingReference()));
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			LOGGER.error("Unable to send reservation confirmation email for {} ({}).",
					event.bookingReference(),
					exception.getClass().getSimpleName());
		}
	}
}
