package org.ryanair.flight.api;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class RyanairFlightConnectingServiceRunner {
    public static void main(String[] args) {
        SpringApplication.run(RyanairFlightConnectingServiceRunner.class , args);    }
}