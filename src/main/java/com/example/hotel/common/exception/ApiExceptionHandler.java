package com.example.hotel.common.exception;

import java.net.URI;

import jakarta.validation.ConstraintViolationException;
import com.example.hotel.module.hotel.service.HotelSettingsService.HotelSettingsNotFoundException;
import com.example.hotel.module.guest.service.GuestService.InvalidGuestSearchException;
import com.example.hotel.module.reservation.service.ReservationService.InvalidReservationRequestException;
import com.example.hotel.module.reservation.service.ReservationService.RoomUnavailableException;
import com.example.hotel.module.reservation.service.ReservationService.ReservationNotFoundException;
import com.example.hotel.module.payment.stripe.StripeCheckoutNotFoundException;
import com.example.hotel.module.payment.stripe.StripeGatewayException;
import com.example.hotel.module.payment.stripe.StripePaymentNotConfiguredException;
import com.example.hotel.module.payment.stripe.StripePaymentProviderException;
import com.example.hotel.module.payment.stripe.StripeWebhookSignatureException;
import com.example.hotel.module.room.service.RoomInventoryService.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler({
			HotelSettingsNotFoundException.class,
			ResourceNotFoundException.class,
			ReservationNotFoundException.class,
			StripeCheckoutNotFoundException.class
	})
	ProblemDetail handleNotFound(RuntimeException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflict() {
		return ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, "The request conflicts with an existing record or database constraint");
	}

	@ExceptionHandler(RoomUnavailableException.class)
	ProblemDetail handleRoomUnavailable(RoomUnavailableException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(InvalidReservationRequestException.class)
	ProblemDetail handleInvalidReservation(InvalidReservationRequestException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail handleInvalidReservationState(IllegalStateException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(StripeWebhookSignatureException.class)
	ProblemDetail handleInvalidStripeSignature() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Stripe webhook signature is invalid");
	}

	@ExceptionHandler(StripePaymentNotConfiguredException.class)
	ProblemDetail handleStripeNotConfigured() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Stripe payments are not configured");
	}

	@ExceptionHandler({StripePaymentProviderException.class, StripeGatewayException.class})
	ProblemDetail handleStripeProviderFailure() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Stripe payment processing failed");
	}

	@ExceptionHandler(InvalidGuestSearchException.class)
	ProblemDetail handleInvalidGuestSearch(InvalidGuestSearchException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request parameters are invalid");
	}
}
