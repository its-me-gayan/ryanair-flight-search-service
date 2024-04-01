package org.ryanair.flight.api.client.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ryanair.flight.api.client.APIClient;
import org.ryanair.flight.api.config.property.RyanairBackEndEndpointConfiguration;
import org.ryanair.flight.api.exception.BackendInvocationException;
import org.ryanair.flight.api.model.RouteAPIResponseModel;
import org.ryanair.flight.api.model.ScheduleAPIRequestModel;
import org.ryanair.flight.api.model.ScheduleAPIResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Implementation of the APIClient interface to interact with the Ryanair backend services.
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RyanairAPIClient implements APIClient {


    private final WebClient webClient;
    private final RyanairBackEndEndpointConfiguration backEndEndpointConfiguration;


    /**
     * Retrieves a list of available routes from the Ryanair backend.
     *
     * @return A Mono emitting a list of RouteAPIResponseModel instances.
     * @throws BackendInvocationException if there's an error invoking the backend service.
     */
    @Override
    public Mono<List<RouteAPIResponseModel>> getRoutes() throws BackendInvocationException {

        return webClient.get()
                .uri(backEndEndpointConfiguration.getRouteEndpointURL())
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful() , clientResponse -> Mono.error(new BackendInvocationException("Invalid Response from backend - "+clientResponse.statusCode())))
                .bodyToFlux(RouteAPIResponseModel.class)
                .collectList();
    }


    /**
     * Retrieves flight schedules from the Ryanair backend based on the provided schedule request model.
     *
     * @param scheduleAPIRequestModel The schedule request model containing departure, arrival, year, and month information.
     * @return A Mono emitting a ScheduleAPIResponseModel instance.
     * @throws BackendInvocationException if there's an error invoking the backend service.
     */
    @Override
    public Mono<ScheduleAPIResponseModel> getSchedules(ScheduleAPIRequestModel scheduleAPIRequestModel) throws BackendInvocationException {
        return webClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(backEndEndpointConfiguration.getScheduleEndpointURL())
                                .build(
                                        scheduleAPIRequestModel.getDeparture(),
                                        scheduleAPIRequestModel.getArrival(),
                                        scheduleAPIRequestModel.getYear(),
                                        scheduleAPIRequestModel.getMonth()
                                )

                )
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful(), clientResponse -> Mono.error(new BackendInvocationException("Invalid Response from backend - " + clientResponse.statusCode())))
                .bodyToMono(ScheduleAPIResponseModel.class);

    }

}
