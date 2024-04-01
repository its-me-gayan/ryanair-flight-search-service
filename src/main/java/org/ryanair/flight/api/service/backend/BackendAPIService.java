package org.ryanair.flight.api.service.backend;

import org.ryanair.flight.api.exception.BackendInvocationException;
import org.ryanair.flight.api.model.RouteAPIResponseModel;
import org.ryanair.flight.api.model.ScheduleAPIRequestModel;
import org.ryanair.flight.api.model.ScheduleAPIResponseModel;
import reactor.core.publisher.Mono;

import java.util.List;
/**
 * Service interface defining methods behaviours to interact with the backend API.
 * This is an Intermediate service layer to separate downstream integration from the main business service layer
 */
public interface BackendAPIService {

    /**
     * Retrieves routes based on arrival and departure IATA codes.
     *
     * @param arrivalIATACode   The IATA code of the arrival airport.
     * @param departureIATACode The IATA code of the departure airport.
     * @return A Mono emitting a list of RouteAPIResponseModel objects.
     * @throws BackendInvocationException if there's an error invoking the backend API.
     */
     Mono<List<RouteAPIResponseModel>> getRoutes(String arrivalIATACode , String departureIATACode) throws BackendInvocationException;


     /**
     * Retrieves schedules based on the provided ScheduleAPIRequestModel.
     *
     * @param scheduleAPIRequestModel The request model containing schedule parameters.
     * @return A Mono emitting a ScheduleAPIResponseModel object.
     * @throws BackendInvocationException if there's an error invoking the backend API.
     */
     Mono<ScheduleAPIResponseModel> getSchedules(ScheduleAPIRequestModel scheduleAPIRequestModel) throws BackendInvocationException;
}
