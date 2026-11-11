package com.example.hotel.module.hotel.service;

import java.util.Optional;

import com.example.hotel.module.hotel.dto.HotelSettingsRequest;
import com.example.hotel.module.hotel.dto.HotelSettingsResponse;
import com.example.hotel.module.hotel.entity.HotelSettings;
import com.example.hotel.module.hotel.repository.HotelSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelSettingsService {

	private static final short SETTINGS_ID = 1;
	private final HotelSettingsRepository repository;

	public HotelSettingsService(HotelSettingsRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public HotelSettingsResponse getSettings() {
		HotelSettings settings = repository.findById(SETTINGS_ID)
				.orElseThrow(() -> new HotelSettingsNotFoundException());
		return toResponse(settings);
	}

	@Transactional
	public HotelSettingsResponse saveSettings(HotelSettingsRequest request) {
		HotelSettings settings = repository.findById(SETTINGS_ID)
				.orElseGet(() -> new HotelSettings(
						request.hotelName(),
						request.streetAddress(),
						request.phoneNumber(),
						request.contactEmail(),
						request.timeZone(),
						request.currencyCode()));
		settings.update(
				request.hotelName(),
				request.streetAddress(),
				request.phoneNumber(),
				request.contactEmail(),
				request.timeZone(),
				request.currencyCode());
		return toResponse(repository.save(settings));
	}

	private static HotelSettingsResponse toResponse(HotelSettings settings) {
		return new HotelSettingsResponse(
				settings.getHotelName(),
				settings.getStreetAddress(),
				settings.getPhoneNumber(),
				settings.getContactEmail(),
				settings.getTimeZone(),
				settings.getCurrencyCode(),
				settings.getUpdatedAt());
	}

	public static class HotelSettingsNotFoundException extends RuntimeException {

		public HotelSettingsNotFoundException() {
			super("Hotel settings have not been configured");
		}
	}
}
