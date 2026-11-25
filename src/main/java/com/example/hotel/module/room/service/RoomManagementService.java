package com.example.hotel.module.room.service;

import java.util.List;
import java.util.Locale;

import com.example.hotel.module.reservation.repository.RoomAllocationRepository;
import com.example.hotel.module.room.dto.HotelRoomRequest;
import com.example.hotel.module.room.dto.HotelRoomResponse;
import com.example.hotel.module.room.dto.HotelRoomStatusRequest;
import com.example.hotel.module.room.dto.HotelRoomUpdateRequest;
import com.example.hotel.module.room.dto.RoomTypeActiveRequest;
import com.example.hotel.module.room.dto.RoomTypeRequest;
import com.example.hotel.module.room.dto.RoomTypeResponse;
import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.entity.RoomType;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.enums.RoomCategory;
import com.example.hotel.module.room.enums.RoomImageKey;
import com.example.hotel.module.room.repository.RoomRepository;
import com.example.hotel.module.room.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomManagementService {

	private final RoomTypeRepository roomTypeRepository;
	private final RoomRepository roomRepository;
	private final RoomAllocationRepository roomAllocationRepository;

	public RoomManagementService(
			RoomTypeRepository roomTypeRepository,
			RoomRepository roomRepository,
			RoomAllocationRepository roomAllocationRepository) {
		this.roomTypeRepository = roomTypeRepository;
		this.roomRepository = roomRepository;
		this.roomAllocationRepository = roomAllocationRepository;
	}

	@Transactional(readOnly = true)
	public List<RoomTypeResponse> listRoomTypes() {
		return roomTypeRepository.findAllByOrderByCodeAsc().stream()
				.map(RoomManagementService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public RoomTypeResponse getRoomType(Long id) {
		return toResponse(findRoomType(id));
	}

	@Transactional
	public RoomTypeResponse createRoomType(RoomTypeRequest request) {
		validateCategoryImage(request.category(), request.imageKey());
		String code = normalizeCode(request.code());
		if (roomTypeRepository.existsByCodeIgnoreCase(code)) {
			throw new IllegalStateException("A room type with this code already exists");
		}
		RoomType roomType = new RoomType(
				code,
				request.name().trim(),
				request.category(),
				request.imageKey(),
				normalizeDescription(request.description()),
				request.maxOccupancy().shortValue(),
				request.roomLimit(),
				request.baseNightlyRate());
		return toResponse(roomTypeRepository.save(roomType));
	}

	@Transactional
	public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request) {
		validateCategoryImage(request.category(), request.imageKey());
		RoomType roomType = findRoomTypeForUpdate(id);
		List<HotelRoom> rooms = roomRepository.findAllByRoomTypeIdForUpdate(id);
		String code = normalizeCode(request.code());
		if (roomTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
			throw new IllegalStateException("A room type with this code already exists");
		}
		if (request.roomLimit() < rooms.size()) {
			throw new IllegalStateException("Room limit cannot be lower than the number of existing rooms");
		}

		boolean bookingTermsChanged = roomType.getCategory() != request.category()
				|| roomType.getMaxOccupancy() != request.maxOccupancy()
				|| roomType.getBaseNightlyRate().compareTo(request.baseNightlyRate()) != 0;
		if (bookingTermsChanged && roomAllocationRepository.existsByRoom_RoomType_IdAndActiveTrue(id)) {
			throw new IllegalStateException(
					"Category, capacity, and nightly rate cannot change while this room type has active reservations");
		}

		roomType.update(
				code,
				request.name().trim(),
				request.category(),
				request.imageKey(),
				normalizeDescription(request.description()),
				request.maxOccupancy().shortValue(),
				request.roomLimit(),
				request.baseNightlyRate());
		return toResponse(roomType);
	}

	@Transactional
	public RoomTypeResponse setRoomTypeActive(Long id, RoomTypeActiveRequest request) {
		RoomType roomType = findRoomTypeForUpdate(id);
		roomRepository.findAllByRoomTypeIdForUpdate(id);
		roomType.setActive(request.active());
		return toResponse(roomType);
	}

	@Transactional(readOnly = true)
	public List<HotelRoomResponse> listRooms() {
		return roomRepository.findAllByOrderByRoomNumberAsc().stream()
				.map(RoomManagementService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public HotelRoomResponse getRoom(Long id) {
		return toResponse(findRoom(id));
	}

	@Transactional
	public HotelRoomResponse createRoom(HotelRoomRequest request) {
		RoomType roomType = findRoomTypeForUpdate(request.roomTypeId());
		if (!roomType.isActive()) {
			throw new IllegalStateException("Rooms can only be added to an active room type");
		}
		List<HotelRoom> rooms = roomRepository.findAllByRoomTypeIdForUpdate(roomType.getId());
		if (rooms.size() >= roomType.getRoomLimit()) {
			throw new IllegalStateException("The room type has reached its room limit");
		}
		String roomNumber = normalizeRoomNumber(request.roomNumber());
		if (roomRepository.existsByRoomNumberIgnoreCase(roomNumber)) {
			throw new IllegalStateException("A room with this number already exists");
		}
		HotelRoom room = new HotelRoom(roomNumber, roomType, request.operationalStatus());
		return toResponse(roomRepository.save(room));
	}

	@Transactional
	public HotelRoomResponse updateRoom(Long id, HotelRoomUpdateRequest request) {
		HotelRoom room = roomRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new HotelRoomNotFoundException(id));
		String roomNumber = normalizeRoomNumber(request.roomNumber());
		if (roomRepository.existsByRoomNumberIgnoreCaseAndIdNot(roomNumber, id)) {
			throw new IllegalStateException("A room with this number already exists");
		}
		validateRoomStatusChange(room, request.operationalStatus());
		room.update(roomNumber, room.getRoomType(), request.operationalStatus());
		return toResponse(room);
	}

	@Transactional
	public HotelRoomResponse setRoomOperationalStatus(Long id, HotelRoomStatusRequest request) {
		HotelRoom room = roomRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new HotelRoomNotFoundException(id));
		validateRoomStatusChange(room, request.operationalStatus());
		room.setOperationalStatus(request.operationalStatus());
		return toResponse(room);
	}

	private void validateRoomStatusChange(
			HotelRoom room,
			OperationalStatus status) {
		if (status == OperationalStatus.OUT_OF_SERVICE
				&& room.getOperationalStatus() != OperationalStatus.OUT_OF_SERVICE
				&& roomAllocationRepository.existsByRoom_IdAndActiveTrue(room.getId())) {
			throw new IllegalStateException("A room with active reservations cannot be taken out of service");
		}
	}

	private RoomType findRoomType(Long id) {
		return roomTypeRepository.findById(id).orElseThrow(() -> new RoomTypeNotFoundException(id));
	}

	private RoomType findRoomTypeForUpdate(Long id) {
		return roomTypeRepository.findByIdForUpdate(id).orElseThrow(() -> new RoomTypeNotFoundException(id));
	}

	private HotelRoom findRoom(Long id) {
		return roomRepository.findById(id).orElseThrow(() -> new HotelRoomNotFoundException(id));
	}

	private static RoomTypeResponse toResponse(RoomType roomType) {
		return new RoomTypeResponse(
				roomType.getId(),
				roomType.getCode(),
				roomType.getName(),
				roomType.getCategory(),
				roomType.getImageKey(),
				roomType.getDescription(),
				roomType.getMaxOccupancy(),
				roomType.getRoomLimit(),
				roomType.getBaseNightlyRate(),
				roomType.isActive(),
				roomType.getCreatedAt(),
				roomType.getUpdatedAt());
	}

	private static HotelRoomResponse toResponse(HotelRoom room) {
		RoomType roomType = room.getRoomType();
		return new HotelRoomResponse(
				room.getId(),
				room.getRoomNumber(),
				roomType.getId(),
				roomType.getCode(),
				roomType.getName(),
				room.getOperationalStatus(),
				room.getCreatedAt(),
				room.getUpdatedAt());
	}

	private static String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeRoomNumber(String roomNumber) {
		return roomNumber.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}

	private static void validateCategoryImage(RoomCategory category, RoomImageKey imageKey) {
		if (imageKey == RoomImageKey.ROOM_PLACEHOLDER) {
			return;
		}
		boolean dormitoryImage = imageKey.name().startsWith("DRM_");
		if ((category == RoomCategory.DORMITORY) != dormitoryImage) {
			throw new InvalidRoomManagementRequestException(
					"Room image key must match the selected room category");
		}
	}

	public static class RoomTypeNotFoundException extends RuntimeException {
		public RoomTypeNotFoundException(Long id) {
			super("Room type " + id + " was not found");
		}
	}

	public static class HotelRoomNotFoundException extends RuntimeException {
		public HotelRoomNotFoundException(Long id) {
			super("Room " + id + " was not found");
		}
	}

	public static class InvalidRoomManagementRequestException extends RuntimeException {
		public InvalidRoomManagementRequestException(String message) {
			super(message);
		}
	}
}
