package com.example.hotel.module.room.service;

import com.example.hotel.module.room.dto.RoomRequest;
import com.example.hotel.module.room.dto.RoomResponse;
import com.example.hotel.module.room.dto.RoomTypeRequest;
import com.example.hotel.module.room.dto.RoomTypeResponse;
import com.example.hotel.module.room.dto.RoomUpdateRequest;
import com.example.hotel.module.room.entity.HotelRoom;
import com.example.hotel.module.room.entity.RoomType;
import com.example.hotel.module.room.enums.HousekeepingStatus;
import com.example.hotel.module.room.enums.OperationalStatus;
import com.example.hotel.module.room.repository.RoomRepository;
import com.example.hotel.module.room.repository.RoomTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomInventoryService {

	private final RoomTypeRepository roomTypeRepository;
	private final RoomRepository roomRepository;

	public RoomInventoryService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository) {
		this.roomTypeRepository = roomTypeRepository;
		this.roomRepository = roomRepository;
	}

	@Transactional(readOnly = true)
	public Page<RoomTypeResponse> listRoomTypes(Pageable pageable) {
		return roomTypeRepository.findAll(pageable).map(RoomInventoryService::toResponse);
	}

	@Transactional
	public RoomTypeResponse createRoomType(RoomTypeRequest request) {
		RoomType roomType = new RoomType(
				request.code(),
				request.name(),
				request.description(),
				request.maxOccupancy(),
				request.baseNightlyRate(),
				request.active());
		return toResponse(roomTypeRepository.save(roomType));
	}

	@Transactional
	public RoomTypeResponse updateRoomType(long id, RoomTypeRequest request) {
		RoomType roomType = roomTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Room type", id));
		roomType.update(
				request.code(),
				request.name(),
				request.description(),
				request.maxOccupancy(),
				request.baseNightlyRate(),
				request.active());
		return toResponse(roomType);
	}

	@Transactional(readOnly = true)
	public Page<RoomResponse> searchRooms(
			Long roomTypeId,
			String floorLabel,
			OperationalStatus operationalStatus,
			HousekeepingStatus housekeepingStatus,
			Pageable pageable) {
		return roomRepository.search(roomTypeId, operationalStatus, housekeepingStatus, floorLabel, pageable)
				.map(RoomInventoryService::toResponse);
	}

	@Transactional
	public RoomResponse createRoom(RoomRequest request) {
		RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
				.orElseThrow(() -> new ResourceNotFoundException("Room type", request.roomTypeId()));
		HotelRoom room = new HotelRoom(
				request.roomNumber(),
				roomType,
				request.floorLabel(),
				request.operationalStatus(),
				request.housekeepingStatus());
		return toResponse(roomRepository.save(room));
	}

	@Transactional
	public RoomResponse updateRoom(long id, RoomUpdateRequest request) {
		HotelRoom room = roomRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Room", id));
		RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
				.orElseThrow(() -> new ResourceNotFoundException("Room type", request.roomTypeId()));
		room.update(
				request.roomNumber(),
				roomType,
				request.floorLabel(),
				request.operationalStatus(),
				request.housekeepingStatus());
		return toResponse(room);
	}

	private static RoomTypeResponse toResponse(RoomType roomType) {
		return new RoomTypeResponse(
				roomType.getId(),
				roomType.getCode(),
				roomType.getName(),
				roomType.getDescription(),
				roomType.getMaxOccupancy(),
				roomType.getBaseNightlyRate(),
				roomType.isActive());
	}

	private static RoomResponse toResponse(HotelRoom room) {
		return new RoomResponse(
				room.getId(),
				room.getRoomNumber(),
				room.getRoomType().getId(),
				room.getRoomType().getCode(),
				room.getRoomType().getName(),
				room.getFloorLabel(),
				room.getOperationalStatus(),
				room.getHousekeepingStatus());
	}

	public static class ResourceNotFoundException extends RuntimeException {

		public ResourceNotFoundException(String resource, Object id) {
			super(resource + " not found: " + id);
		}
	}
}
