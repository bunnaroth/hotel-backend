package com.example.hotel.module.reservation.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.hotel.module.guest.dto.GuestRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "ReservationCreateRequest", description = "Public request to reserve one or more rooms or dormitory beds. The stay uses an inclusive check-in and exclusive check-out date.")
public record ReservationCreateRequest(
		@Schema(description = "Check-in date in ISO-8601 format; must be before checkOutDate.", example = "2032-03-10")
		@NotNull LocalDate checkInDate,
		@Schema(description = "Check-out date in ISO-8601 format; the guest checks out on this date.", example = "2032-03-12")
		@NotNull LocalDate checkOutDate,
		@Schema(description = "Primary guest. At least one contact method is required.")
		@NotNull @Valid GuestRequest primaryGuest,
		@Schema(description = "Optional additional guests included in this reservation.")
		List<@NotNull @Valid GuestRequest> additionalGuests,
		@Schema(description = "Room selections. Required unless the deprecated roomIds field is provided. Private room quantity must be 1; dormitory quantity is the number of beds.")
		List<@NotNull @Valid RoomSelectionRequest> roomSelections,
		@Schema(description = "Legacy whole-room selection; prefer roomSelections.", deprecated = true)
		List<@NotNull @Positive Long> roomIds) {

	@AssertTrue(message = "Check-in date must be before check-out date")
	public boolean isDateRangeValid() {
		return checkInDate != null && checkOutDate != null && checkInDate.isBefore(checkOutDate);
	}

	@AssertTrue(message = "At least one room selection is required")
	public boolean isRoomSelectionProvided() {
		return roomSelections != null && !roomSelections.isEmpty()
				|| roomIds != null && !roomIds.isEmpty();
	}
}
