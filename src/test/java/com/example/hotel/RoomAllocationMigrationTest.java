package com.example.hotel;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomAllocationMigrationTest {

	@Test
	void migrationSucceedsWhenAllocatedUnitsAlreadyExists() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:room-allocation-migration;DB_CLOSE_DELAY=-1",
				"sa",
				"");
		dataSource.setDriverClassName("org.h2.Driver");
		try (var connection = dataSource.getConnection();
			 var statement = connection.createStatement()) {
			statement.execute("""
					CREATE TABLE room_allocation (
					    allocation_id BIGINT PRIMARY KEY,
					    allocated_units INTEGER NOT NULL DEFAULT 1
					        CHECK (allocated_units > 0)
					)
					""");
		} catch (java.sql.SQLException exception) {
			throw new IllegalStateException("Could not prepare the migration test database", exception);
		}

		Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:db/migration")
				.baselineOnMigrate(true)
				.baselineVersion("2")
				.load()
				.migrate();

		assertEquals("3", Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:db/migration")
				.load()
				.info()
				.current()
				.getVersion()
				.getVersion());
	}
}
