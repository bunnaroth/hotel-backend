package com.example.hotel.module.reservation.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import com.example.hotel.module.reservation.dto.ReservationLookupChallengeResponse;
import com.example.hotel.module.reservation.dto.ReservationLookupRoomResponse;
import com.example.hotel.module.reservation.dto.ReservationLookupResponse;
import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.entity.ReservationLookupChallenge;
import com.example.hotel.module.reservation.entity.ReservationPayment;
import com.example.hotel.module.reservation.entity.RoomAllocation;
import com.example.hotel.module.reservation.repository.ReservationLookupChallengeRepository;
import com.example.hotel.module.reservation.repository.ReservationPaymentRepository;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.entity.RoomType;
import com.example.hotel.module.room.enums.RoomCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationLookupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReservationLookupService.class);
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
	private static final Duration CLEANUP_RETENTION = Duration.ofDays(1);
	private static final int MAX_CODE_ATTEMPTS = 5;
	private static final String REQUEST_MESSAGE =
			"Verification code has been sent.";

	private final ReservationLookupChallengeRepository challengeRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationPaymentRepository paymentRepository;
	private final RoomAllocationRepository roomAllocationRepository;
	private final JavaMailSender mailSender;
	private final SecretKey signingKey;
	private final String senderAddress;
	private final int maxRequestsPerLookup;
	private final int maxRequestsPerClientIp;

	public ReservationLookupService(
			ReservationLookupChallengeRepository challengeRepository,
			ReservationRepository reservationRepository,
			ReservationPaymentRepository paymentRepository,
			RoomAllocationRepository roomAllocationRepository,
			JavaMailSender mailSender,
			SecretKey signingKey,
			@Value("${app.mail.from:no-reply@example.com}") String senderAddress,
			@Value("${app.reservation-lookup.max-requests-per-lookup-per-hour:3}")
					int maxRequestsPerLookup,
			@Value("${app.reservation-lookup.max-requests-per-ip-per-hour:10}")
					int maxRequestsPerClientIp) {
		if (maxRequestsPerLookup < 1 || maxRequestsPerClientIp < 1) {
			throw new IllegalArgumentException("Reservation lookup rate limits must be positive");
		}
		this.challengeRepository = challengeRepository;
		this.reservationRepository = reservationRepository;
		this.paymentRepository = paymentRepository;
		this.roomAllocationRepository = roomAllocationRepository;
		this.mailSender = mailSender;
		this.signingKey = signingKey;
		this.senderAddress = senderAddress;
		this.maxRequestsPerLookup = maxRequestsPerLookup;
		this.maxRequestsPerClientIp = maxRequestsPerClientIp;
	}

	@Transactional
	public ReservationLookupChallengeResponse requestCode(
			String bookingReference,
			String email,
			String clientIp) {
		String normalizedReference = bookingReference.trim().toUpperCase(Locale.ROOT);
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		Instant now = Instant.now();
		String lookupHash = hmac("lookup:" + normalizedReference + "\n" + normalizedEmail);
		String clientIpHash = hmac("client-ip:" + clientIp);
		Instant rateLimitWindowStart = now.minus(RATE_LIMIT_WINDOW);

		if (challengeRepository.countByClientIpHashAndCreatedAtAfter(
				clientIpHash, rateLimitWindowStart) >= maxRequestsPerClientIp
				|| challengeRepository.countByLookupHashAndCreatedAtAfter(
						lookupHash, rateLimitWindowStart) >= maxRequestsPerLookup) {
			throw new ReservationLookupRateLimitException();
		}

		UUID challengeId = UUID.randomUUID();
		String code = String.format(Locale.ROOT, "%08d", RANDOM.nextInt(100_000_000));
		Reservation reservation = reservationRepository
				.findForPublicLookup(normalizedReference, normalizedEmail)
				.orElse(null);
		ReservationLookupChallenge challenge = new ReservationLookupChallenge(
				challengeId,
				reservation == null ? null : reservation.getId(),
				lookupHash,
				clientIpHash,
				hmac(codeHashInput(challengeId, code)),
				now.plus(CODE_TTL));
		challengeRepository.save(challenge);

		if (reservation != null) {
			sendCode(normalizedEmail, challenge, code);
		}
		return new ReservationLookupChallengeResponse(challengeId, REQUEST_MESSAGE);
	}

	@Transactional(noRollbackFor = InvalidReservationLookupException.class)
	public ReservationLookupResponse verifyCode(UUID challengeId, String code) {
		Instant now = Instant.now();
		ReservationLookupChallenge challenge = challengeRepository.findByIdForUpdate(challengeId)
				.orElseThrow(InvalidReservationLookupException::new);
		if (!challenge.isUsableAt(now, MAX_CODE_ATTEMPTS)) {
			throw new InvalidReservationLookupException();
		}

		String candidateHash = hmac(codeHashInput(challengeId, code));
		boolean codeMatches = MessageDigest.isEqual(
				challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII),
				candidateHash.getBytes(StandardCharsets.US_ASCII));
		if (!codeMatches || challenge.getReservationId() == null) {
			challenge.recordFailedAttempt();
			throw new InvalidReservationLookupException();
		}

		challenge.consume(now);
		Reservation reservation = reservationRepository.findById(challenge.getReservationId())
				.orElseThrow(InvalidReservationLookupException::new);
		List<RoomAllocation> allocations = roomAllocationRepository.findAllByReservation_Id(reservation.getId());
		Map<String, ReservationLookupRoomResponse> roomSummaries = new LinkedHashMap<>();
		for (RoomAllocation allocation : allocations) {
			RoomType roomType = allocation.getRoom().getRoomType();
			int quantity = roomType.getCategory() == RoomCategory.DORMITORY
					? allocation.getAllocatedUnits()
					: 1;
			ReservationLookupRoomResponse roomSummary = new ReservationLookupRoomResponse(
					roomType.getCode(),
					roomType.getName(),
					roomType.getCategory(),
					roomType.getImageKey(),
					quantity);
			roomSummaries.merge(
					roomType.getCode(),
					roomSummary,
					(existing, added) -> new ReservationLookupRoomResponse(
							existing.roomTypeCode(),
							existing.roomTypeName(),
							existing.category(),
							existing.imageKey(),
							existing.quantity() + added.quantity()));
		}
		List<ReservationLookupRoomResponse> rooms = roomSummaries.values().stream()
				.sorted(Comparator.comparing(ReservationLookupRoomResponse::roomTypeName))
				.toList();
		ReservationPayment payment = paymentRepository
				.findFirstByReservation_IdOrderByCreatedAtDesc(reservation.getId())
				.orElse(null);
		return new ReservationLookupResponse(
				reservation.getBookingReference(),
				reservation.getCheckInDate(),
				reservation.getCheckOutDate(),
				reservation.getStatus(),
				rooms,
				payment == null ? null : payment.getStatus());
	}

	@Scheduled(cron = "${app.reservation-lookup.cleanup-cron:0 29 3 * * *}")
	@Transactional
	public void deleteExpiredChallenges() {
		challengeRepository.deleteExpiredBefore(Instant.now().minus(CLEANUP_RETENTION));
	}

	private void sendCode(
			String email,
			ReservationLookupChallenge challenge,
			String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(senderAddress);
		message.setTo(email);
		message.setSubject("Your reservation verification code");
		message.setText("""
				Use this code to view your reservation:

				%s

				The code expires in 10 minutes. If you did not request it, you can ignore this email.
				""".formatted(code));
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			challenge.consume(Instant.now());
			LOGGER.error("Unable to send reservation lookup email ({}).",
					exception.getClass().getSimpleName());
		}
	}

	private String hmac(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(signingKey);
			return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Reservation lookup hashing is unavailable", exception);
		}
	}

	private static String codeHashInput(UUID challengeId, String code) {
		return "lookup-code:" + challengeId + ":" + code;
	}
}
