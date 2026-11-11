package com.example.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelManagementBackendApplication.class, args);
	}

}
