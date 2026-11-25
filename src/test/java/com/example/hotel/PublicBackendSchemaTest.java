package com.example.hotel;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.example.hotel.module.reservation.entity.Reservation;
import com.example.hotel.module.reservation.repository.ReservationRepository;
import com.example.hotel.module.reservation.service.ReservationPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:public-backend;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.flyway.locations=classpath:db/migration",
		"app.reservation-lookup.hmac-key=0123456789abcdef0123456789abcdef",
		"app.security.admin.username=test-admin",
		"app.security.admin.password=test-admin-password-123"
})
class PublicBackendSchemaTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ReservationPricingService reservationPricingService;

	@Test
	void flywayCreatesThePublicBookingSchema() {
		assertEquals(9, jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'PUBLIC'
				  AND table_name IN (
				      'HOTEL_SETTINGS',
				      'ROOM_TYPE',
				      'HOTEL_ROOM',
				      'GUEST',
				      'RESERVATION',
				      'RESERVATION_GUEST',
				      'ROOM_ALLOCATION',
				      'RESERVATION_PAYMENT',
				      'RESERVATION_LOOKUP_CHALLENGE'
				  )
				""", Integer.class));
	}

	@Test
	void publicAvailabilityReturnsSeededRoomTypes() throws Exception {
		Long roomId = insertAvailableRoom();

		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2032-03-10")
						.param("checkOutDate", "2032-03-12")
						.param("guestCount", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].roomId").value(roomId.intValue()))
				.andExpect(jsonPath("$.content[0].category").value("PRIVATE"))
				.andExpect(jsonPath("$.content[0].imageKey").value("PVT_SD"))
				.andExpect(jsonPath("$.content[0].availableUnits").value(1));
	}

	@Test
	void openApiAndSwaggerUiArePublished() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.paths['/api/availability']").exists())
				.andExpect(jsonPath("$.components.securitySchemes.adminBasicAuth.scheme").value("basic"))
				.andExpect(jsonPath("$.paths['/api/admin/rooms'].post.security[0].adminBasicAuth").exists())
				.andExpect(jsonPath("$.paths['/api/admin/room-types'].post.description").exists())
				.andExpect(jsonPath("$.components.schemas.RoomTypeRequest.properties.baseNightlyRate.description").exists())
				.andExpect(jsonPath("$.components.schemas.ReservationCreateRequest.properties.roomSelections.description").exists());

		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void adminInventoryEndpointsRequireBasicAuthenticationAndSupportDeactivation() throws Exception {
		mockMvc.perform(get("/api/admin/room-types"))
				.andExpect(status().isUnauthorized());

		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		Long roomTypeId = createRoomType(suffix, "120.00", 1, 2);
		Long roomId = createRoom(roomTypeId, "A-" + suffix);

		mockMvc.perform(get("/api/admin/rooms/" + roomId)
						.header("Authorization", adminAuthorization()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomTypeId").value(roomTypeId.intValue()));

		mockMvc.perform(put("/api/admin/room-types/" + roomTypeId)
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content(roomTypeRequest(suffix, "Updated room type", "120.00", 1, 2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated room type"));

		mockMvc.perform(patch("/api/admin/room-types/" + roomTypeId + "/active")
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(patch("/api/admin/rooms/" + roomId + "/operational-status")
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"operationalStatus\":\"OUT_OF_SERVICE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operationalStatus").value("OUT_OF_SERVICE"));
	}

	@Test
	void activeReservationsPreventChangesToBookingTermsAndRoomDeactivation() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		Long roomTypeId = createRoomType(suffix, "120.00", 1, 2);
		Long roomId = createRoom(roomTypeId, "B-" + suffix);

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest(suffix.toLowerCase(), roomId, 1)))
				.andExpect(status().isCreated());

		mockMvc.perform(put("/api/admin/room-types/" + roomTypeId)
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content(roomTypeRequest(suffix, "Test room", "121.00", 1, 2)))
				.andExpect(status().isConflict());

		mockMvc.perform(patch("/api/admin/rooms/" + roomId + "/operational-status")
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"operationalStatus\":\"OUT_OF_SERVICE\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void publicReservationCanBeCreatedForAnAvailableRoom() throws Exception {
		Long roomId = insertAvailableRoom();
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "2032-03-10",
								  "checkOutDate": "2032-03-12",
								  "primaryGuest": {
								    "firstName": "Test",
								    "lastName": "Guest",
								    "email": "booking-%s@example.test"
								  },
								  "additionalGuests": [],
								  "roomIds": [%d]
								}
								""".formatted(suffix, roomId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.roomIds[0]").value(roomId.intValue()));
	}

	@Test
	void dormitoryBedsCanBeAllocatedAcrossReservationsUntilInventoryIsFull() throws Exception {
		Long roomId = insertAvailableRoom("DORMITORY", 8, 8.00);
		Long roomTypeId = jdbcTemplate.queryForObject(
				"SELECT room_type_id FROM hotel_room WHERE room_id = ?", Long.class, roomId);
		String firstSuffix = UUID.randomUUID().toString().substring(0, 8);
		String secondSuffix = UUID.randomUUID().toString().substring(0, 8);
		String thirdSuffix = UUID.randomUUID().toString().substring(0, 8);

		String firstResponse = mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest(firstSuffix, roomId, 3)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String reservationId = com.jayway.jsonpath.JsonPath.read(firstResponse, "$.id");
		Reservation reservation = reservationRepository.findById(UUID.fromString(reservationId)).orElseThrow();
		assertEquals(new BigDecimal("48.00"), reservationPricingService.calculateStayAmount(reservation));

		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2032-03-10")
						.param("checkOutDate", "2032-03-12")
						.param("roomTypeId", roomTypeId.toString())
						.param("guestCount", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].availableUnits").value(5));

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest(secondSuffix, roomId, 5)))
				.andExpect(status().isCreated());
		assertEquals(8, jdbcTemplate.queryForObject(
				"SELECT SUM(allocated_units) FROM room_allocation WHERE room_id = ?",
				Integer.class,
				roomId));

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest(thirdSuffix, roomId, 1)))
				.andExpect(status().isConflict());
	}

	private String reservationRequest(String suffix, Long roomId, int quantity) {
		return """
				{
				  "checkInDate": "2032-03-10",
				  "checkOutDate": "2032-03-12",
				  "primaryGuest": {
				    "firstName": "Test",
				    "lastName": "Guest",
				    "email": "booking-%s@example.test"
				  },
				  "additionalGuests": [],
				  "roomSelections": [{"roomId": %d, "quantity": %d}]
				}
				""".formatted(suffix, roomId, quantity);
	}

	private Long createRoomType(String suffix, String nightlyRate, int roomLimit, int maxOccupancy)
			throws Exception {
		String response = mockMvc.perform(post("/api/admin/room-types")
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content(roomTypeRequest(suffix, "Test room", nightlyRate, roomLimit, maxOccupancy)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");
		return id.longValue();
	}

	private Long createRoom(Long roomTypeId, String roomNumber) throws Exception {
		String response = mockMvc.perform(post("/api/admin/rooms")
						.header("Authorization", adminAuthorization())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "roomNumber": "%s",
								  "roomTypeId": %d,
								  "operationalStatus": "AVAILABLE"
								}
								""".formatted(roomNumber, roomTypeId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");
		return id.longValue();
	}

	private String roomTypeRequest(
			String code,
			String name,
			String nightlyRate,
			int roomLimit,
			int maxOccupancy) {
		return """
				{
				  "code": "%s",
				  "name": "%s",
				  "category": "PRIVATE",
				  "imageKey": "PVT_SD",
				  "description": "Test inventory",
				  "maxOccupancy": %d,
				  "roomLimit": %d,
				  "baseNightlyRate": %s
				}
				""".formatted(code, name, maxOccupancy, roomLimit, nightlyRate);
	}

	private String adminAuthorization() {
		String credentials = "test-admin:test-admin-password-123";
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

	private Long insertAvailableRoom() {
		return insertAvailableRoom("PRIVATE", 2, 120.00);
	}

	private Long insertAvailableRoom(String category, int maxOccupancy, double nightlyRate) {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		String code = "TEST-" + suffix;
		jdbcTemplate.update("""
				INSERT INTO room_type
				    (code, name, category, image_key, max_occupancy, room_limit, base_nightly_rate, active)
				VALUES (?, 'Test room', ?, ?, ?, 1, ?, TRUE)
				""", code, category, category.equals("DORMITORY") ? "DRM_8M" : "PVT_SD",
				maxOccupancy, nightlyRate);
		Long roomTypeId = jdbcTemplate.queryForObject(
				"SELECT room_type_id FROM room_type WHERE code = ?", Long.class, code);
		jdbcTemplate.update("""
				INSERT INTO hotel_room (room_number, room_type_id, operational_status)
				VALUES (?, ?, 'AVAILABLE')
				""", "T-" + suffix, roomTypeId);
		return jdbcTemplate.queryForObject(
				"SELECT room_id FROM hotel_room WHERE room_number = ?", Long.class, "T-" + suffix);
	}
}
