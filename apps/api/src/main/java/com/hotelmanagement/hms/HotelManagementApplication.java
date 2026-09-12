package com.hotelmanagement.hms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Hotel Management System backend.
 *
 * All HMS business modules will remain under the
 * com.hotelmanagement.hms root package so that Spring Boot
 * can discover controllers, services, repositories,
 * configuration classes, and other managed components.
 */
@SpringBootApplication
public class HotelManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelManagementApplication.class, args);
    }
}