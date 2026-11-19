package com.example.hotel;

import java.util.UUID;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
		"app.reservation-lookup.hmac-key=0123456789abcdef0123456789abcdef"
})
class PublicBackendSchemaTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

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
				.andExpect(jsonPath("$.content[0].imageKey").value("PVT_SD"));
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

	private Long insertAvailableRoom() {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		String code = "TEST-" + suffix;
		jdbcTemplate.update("""
				INSERT INTO room_type
				    (code, name, category, image_key, max_occupancy, room_limit, base_nightly_rate, active)
				VALUES (?, 'Test room', 'PRIVATE', 'PVT_SD', 2, 1, 120.00, TRUE)
				""", code);
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
