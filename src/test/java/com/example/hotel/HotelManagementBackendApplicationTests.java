package com.example.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.example.hotel.module.reservation.service.ReservationService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.hotel.module.auth.entity.RoleEntity;
import com.example.hotel.module.auth.entity.UserAccount;
import com.example.hotel.module.auth.repository.RoleRepository;
import com.example.hotel.module.auth.repository.UserAccountRepository;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:hmos;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.flyway.locations=classpath:db/migration",
		"app.security.jwt.secret=0123456789abcdef0123456789abcdef",
		"app.security.login.max-failed-attempts=2"
})
class HotelManagementBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ReservationService reservationService;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublicAndReportsApplicationStatus() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void applicationApiEndpointsAreDeniedUntilAuthorizationIsConfigured() throws Exception {
		mockMvc.perform(get("/api/private"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validLoginReturnsSignedJwtWithAssignedRole() throws Exception {
		String loginName = createUser("RECEPTIONIST");
		String response = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginName":"%s","password":"correct-password"}
								""".formatted(loginName)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = response.split("\"accessToken\":\"", 2)[1].split("\"", 2)[0];
		assertEquals("RECEPTIONIST", jwtDecoder.decode(token)
				.getClaimAsStringList("roles").getFirst());
		mockMvc.perform(get("/api/private").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidCredentialsAreGenericAndLockTheAccountAfterConfiguredAttempts() throws Exception {
		String loginName = createUser("RECEPTIONIST");
		String invalidRequest = """
				{"loginName":"%s","password":"wrong-password"}
				""".formatted(loginName);

		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidRequest))
					.andExpect(status().isUnauthorized())
					.andExpect(content().json("{\"error\":\"Invalid login credentials\"}"));
		}

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginName":"%s","password":"correct-password"}
								""".formatted(loginName)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().json("{\"error\":\"Invalid login credentials\"}"));
	}

	@Test
	void ownerCanConfigureHotelAndRoomInventory() throws Exception {
		String token = loginToken(createUser("OWNER"));
		String authorization = "Bearer " + token;

		mockMvc.perform(put("/api/hotel/settings")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "hotelName":"Example Hotel",
								  "streetAddress":"1 Hotel Road",
								  "phoneNumber":"+1-555-0100",
								  "contactEmail":"desk@example.test",
								  "timeZone":"America/New_York",
								  "currencyCode":"USD"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hotelName").value("Example Hotel"))
				.andExpect(jsonPath("$.currencyCode").value("USD"));

		String roomTypeResponse = mockMvc.perform(post("/api/room-types")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code":"STD",
								  "name":"Standard",
								  "description":"Standard room",
								  "maxOccupancy":2,
								  "baseNightlyRate":125.00,
								  "active":true
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("STD"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		long roomTypeId = extractLong(roomTypeResponse, "id");

		mockMvc.perform(post("/api/rooms")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "roomNumber":"101",
								  "roomTypeId":%d,
								  "floorLabel":"1",
								  "operationalStatus":"AVAILABLE",
								  "housekeepingStatus":"DIRTY"
								}
								""".formatted(roomTypeId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operationalStatus").value("AVAILABLE"))
				.andExpect(jsonPath("$.housekeepingStatus").value("DIRTY"));

		mockMvc.perform(get("/api/rooms")
						.header("Authorization", authorization)
						.param("roomTypeId", Long.toString(roomTypeId))
						.param("operationalStatus", "AVAILABLE")
						.param("housekeepingStatus", "DIRTY"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].roomNumber").value("101"));
	}

	@Test
	void receptionistCannotManageRoomInventory() throws Exception {
		mockMvc.perform(get("/api/rooms")
						.header("Authorization", "Bearer " + loginToken(createUser("RECEPTIONIST"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicReservationHoldsRoomsAndUsesHalfOpenStayDates() throws Exception {
		long roomTypeId = createRoomType();
		long roomId = createRoom(roomTypeId);

		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2030-04-10")
						.param("checkOutDate", "2030-04-12")
						.param("roomTypeId", Long.toString(roomTypeId))
						.param("guestCount", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest("2030-04-10", "2030-04-12", roomId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.bookingReference").isNotEmpty())
				.andExpect(jsonPath("$.guests.length()").value(2))
				.andExpect(jsonPath("$.guests[0].primary").value(true))
				.andExpect(jsonPath("$.guests[1].primary").value(false));

		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2030-04-11")
						.param("checkOutDate", "2030-04-13")
						.param("roomTypeId", Long.toString(roomTypeId))
						.param("guestCount", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2030-04-12")
						.param("checkOutDate", "2030-04-14")
						.param("roomTypeId", Long.toString(roomTypeId))
						.param("guestCount", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest("2030-04-11", "2030-04-13", roomId)))
				.andExpect(status().isConflict());
	}

	@Test
	void expiredPendingReservationReleasesItsRoomAllocation() throws Exception {
		long roomTypeId = createRoomType();
		long roomId = createRoom(roomTypeId);
		String response = mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest("2030-05-10", "2030-05-11", roomId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String reservationId = extractString(response, "id");
		jdbcTemplate.update(
				"UPDATE reservation SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1' MINUTE WHERE reservation_id = ?",
				UUID.fromString(reservationId));

		assertEquals(1, reservationService.expirePendingReservations());
		mockMvc.perform(get("/api/availability")
						.param("checkInDate", "2030-05-10")
						.param("checkOutDate", "2030-05-11")
						.param("roomTypeId", Long.toString(roomTypeId))
						.param("guestCount", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void receptionistCanSearchReservationsAndGuestsButAnonymousUsersCannot() throws Exception {
		long roomTypeId = createRoomType();
		long roomId = createRoom(roomTypeId);
		String reservationJson = mockMvc.perform(post("/api/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reservationRequest("2030-06-10", "2030-06-11", roomId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String reservationId = extractString(reservationJson, "id");
		String receptionistToken = loginToken(createUser("RECEPTIONIST"));

		mockMvc.perform(get("/api/reservations"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/guests").param("query", "Test"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/reservations")
						.header("Authorization", "Bearer " + receptionistToken)
						.param("guestName", "Test")
						.param("status", "PENDING")
						.param("checkInFrom", "2030-06-10")
						.param("checkInTo", "2030-06-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(reservationId))
				.andExpect(jsonPath("$.content[0].guests.length()").value(2));

		mockMvc.perform(get("/api/reservations/{id}", reservationId)
						.header("Authorization", "Bearer " + receptionistToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(reservationId))
				.andExpect(jsonPath("$.roomIds.length()").value(1));

		mockMvc.perform(get("/api/guests")
						.header("Authorization", "Bearer " + receptionistToken)
						.param("query", "Additional"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.content[0].firstName").value("Additional"))
				.andExpect(jsonPath("$.content[0].email").isNotEmpty());

		mockMvc.perform(get("/api/guests")
						.header("Authorization", "Bearer " + receptionistToken)
						.param("query", "T"))
				.andExpect(status().isBadRequest());
	}

	private String loginToken(String loginName) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginName":"%s","password":"correct-password"}
								""".formatted(loginName)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractString(response, "accessToken");
	}

	private long createRoomType() throws Exception {
		String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		String response = mockMvc.perform(post("/api/room-types")
						.header("Authorization", "Bearer " + loginToken(createUser("OWNER")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code":"%s",
								  "name":"Test room",
								  "description":"Reservation test room",
								  "maxOccupancy":2,
								  "baseNightlyRate":125.00,
								  "active":true
								}
								""".formatted(code)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractLong(response, "id");
	}

	private long createRoom(long roomTypeId) throws Exception {
		String roomNumber = UUID.randomUUID().toString().substring(0, 8);
		String response = mockMvc.perform(post("/api/rooms")
						.header("Authorization", "Bearer " + loginToken(createUser("OWNER")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "roomNumber":"%s",
								  "roomTypeId":%d,
								  "operationalStatus":"AVAILABLE",
								  "housekeepingStatus":"CLEAN"
								}
								""".formatted(roomNumber, roomTypeId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractLong(response, "id");
	}

	private static String reservationRequest(String checkIn, String checkOut, long roomId) {
		return """
				{
				  "checkInDate":"%s",
				  "checkOutDate":"%s",
				  "primaryGuest":{
				    "firstName":"Test",
				    "lastName":"Guest",
				    "email":"guest-%s@example.test"
				  },
				  "additionalGuests":[{
				    "firstName":"Additional",
				    "lastName":"Guest",
				    "email":"additional-%s@example.test"
				  }],
				  "roomIds":[%d]
				}
				""".formatted(checkIn, checkOut, UUID.randomUUID(), UUID.randomUUID(), roomId);
	}

	private static long extractLong(String json, String field) {
		return Long.parseLong(extractString(json, field));
	}

	private static String extractString(String json, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\":\"?([^\",}]+)").matcher(json);
		if (!matcher.find()) {
			throw new AssertionError("Missing JSON field: " + field);
		}
		return matcher.group(1);
	}

	private String createUser(String roleName) {
		String loginName = UUID.randomUUID() + "@example.test";
		RoleEntity role = roleRepository.findByRoleName(roleName).orElseThrow();
		userAccountRepository.save(new UserAccount(
				loginName,
				passwordEncoder.encode("correct-password"),
				Set.of(role)));
		return loginName;
	}
}
