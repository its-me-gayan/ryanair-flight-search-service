package org.ryanair.flight.api.service.frontend;

import org.ryanair.flight.api.dto.FlightDataDto;
import org.ryanair.flight.api.dto.ScheduledServiceDto;
import reactor.core.publisher.Mono;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 4/1/24
 * Time: 8:19 PM
 */

/**
 * This interface defines methods behaviors to retrieve scheduled flight data.
 * It provides functionality to get departing, arriving, and direct flight data.
 */
public interface ScheduleService {

    /**
     * Retrieves scheduled departing flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling departing flights.
     * @return A Mono emitting FlightDataDto containing scheduled departing flight data.
     */
    Mono<FlightDataDto>  getScheduledDepartingFlightData(ScheduledServiceDto scheduledServiceDto);

    /**
     * Retrieves scheduled arriving flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling arriving flights.
     * @return A Mono emitting FlightDataDto containing scheduled arriving flight data.
     */
    Mono<FlightDataDto> getScheduledArrivingFlightData(ScheduledServiceDto scheduledServiceDto);

    /**
     * Retrieves scheduled direct flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling direct flights.
     * @return A Mono emitting FlightDataDto containing scheduled direct flight data.
     */
    Mono<FlightDataDto> getScheduledDirectFlightData(ScheduledServiceDto scheduledServiceDto);
}
