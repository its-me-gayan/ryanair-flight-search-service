package org.ryanair.flight.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ryanair.flight.api.client.impl.RyanairAPIClient;
import org.ryanair.flight.api.service.backend.impl.RyanairBackendAPIServiceImpl;
import org.ryanair.flight.api.service.frontend.impl.FlightSearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 4/1/24
 * Time: 1:18 PM
 */

@ExtendWith(SpringExtension.class)
//@WebFluxTest(controllers = FlightInterConnectController.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

//@Import({FlightInterConnectServiceImpl.class,BackendAPIServiceImpl.class , RyanairAPIClient.class})
class FlightInterConnectControllerTest {

    @MockBean
    private FlightSearchServiceImpl flightInterConnectService;

    @MockBean
    private RyanairBackendAPIServiceImpl backendAPIService;

    @MockBean
    private RyanairAPIClient ryanairAPIClient;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void findAvailableFlights() {
String url = "/api/v1/flight/interconnections?departure=DUB&arrival=STN&departureDateTime=2024-06-20T07:00&arrivalDateTime=2024-06-21T21:00";

        WebTestClient.ResponseSpec exchange =
                webTestClient.get().uri(url).exchange();
        exchange.expectStatus()
                .is2xxSuccessful()
                .expectBody()

                .jsonPath("$.responseCode").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Data retrieved successfully")
                .jsonPath("$.data").isNotEmpty();
    }
}