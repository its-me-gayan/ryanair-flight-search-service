package org.ryanair.flight.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class RyanairFlightConnectingServiceRunner {
    public static void main(String[] args) {
        SpringApplication.run(RyanairFlightConnectingServiceRunner.class , args);    }
}