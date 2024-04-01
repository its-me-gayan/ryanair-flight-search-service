package org.ryanair.flight.api.service.frontend.impl;

import lombok.RequiredArgsConstructor;
import org.ryanair.flight.api.dto.FlightDataDto;
import org.ryanair.flight.api.dto.RequestDataDto;
import org.ryanair.flight.api.dto.ScheduledServiceDto;
import org.ryanair.flight.api.dto.YearMonthDataDto;
import org.ryanair.flight.api.model.*;
import org.ryanair.flight.api.service.backend.BackendAPIService;
import org.ryanair.flight.api.service.frontend.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.ryanair.flight.api.util.Constant.DATE_FORMAT_ISO;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 4/1/24
 * Time: 8:19 PM
 * Service implementation for handling scheduled flights.
 */

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ScheduleServiceImpl implements ScheduleService {

    private final BackendAPIService backendAPIService;

    /**
     * Retrieves scheduled departing flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling departing flights.
     * @return A Mono emitting FlightDataDto containing scheduled departing flight data.
     */
    @Override
    public Mono<FlightDataDto> getScheduledDepartingFlightData(ScheduledServiceDto scheduledServiceDto) {

        ScheduleAPIRequestModel departingBuild = ScheduleAPIRequestModel
                .builder()
                .arrival(scheduledServiceDto.getDepartingRouteData().getAirportTo())
                .departure(scheduledServiceDto.getDepartingRouteData().getAirportFrom())
                .year(scheduledServiceDto.getYearMonthData().getYear())
                .month(scheduledServiceDto.getYearMonthData().getMonth())
                .build();


        Mono<ScheduleAPIResponseModel> departingSchedulesMono = backendAPIService.getSchedules(departingBuild);


       return departingSchedulesMono
                .flatMap(scheduleAPIResponseModel ->
                        filterAllAvailableFlightsFromScheduleResponse(
                                scheduledServiceDto.getRequestData(),
                                scheduleAPIResponseModel,
                                scheduledServiceDto.getDepartingRouteData(),
                                scheduledServiceDto.getYearMonthData()
                        )
                );

    }

    /**
     * Retrieves scheduled arriving flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling arriving flights.
     * @return A Mono emitting FlightDataDto containing scheduled arriving flight data.
     */
    @Override
    public Mono<FlightDataDto> getScheduledArrivingFlightData(ScheduledServiceDto scheduledServiceDto) {

        ScheduleAPIRequestModel arrivingBuild = ScheduleAPIRequestModel
                .builder()
                .arrival(scheduledServiceDto.getArrivingRouteData().getAirportTo())
                .departure(scheduledServiceDto.getArrivingRouteData().getAirportFrom())
                .year(scheduledServiceDto.getYearMonthData().getYear())
                .month(scheduledServiceDto.getYearMonthData().getMonth())
                .build();

        Mono<ScheduleAPIResponseModel> arrivingSchedulesMono = backendAPIService.getSchedules(arrivingBuild);

        return arrivingSchedulesMono.flatMap(scheduleAPIResponseModel ->
                filterAllAvailableFlightsFromScheduleResponse(
                        scheduledServiceDto.getRequestData(),
                        scheduleAPIResponseModel,
                        scheduledServiceDto.getArrivingRouteData(),
                        scheduledServiceDto.getYearMonthData()
                )
        );
    }

    /**
     * Retrieves scheduled direct flight data based on the provided ScheduledServiceDto.
     *
     * @param scheduledServiceDto The DTO containing information for scheduling direct flights.
     * @return A Mono emitting FlightDataDto containing scheduled direct flight data.
     */
    @Override
    public Mono<FlightDataDto> getScheduledDirectFlightData(ScheduledServiceDto scheduledServiceDto) {

            ScheduleAPIRequestModel scheduleAPIRequestModel = ScheduleAPIRequestModel
                    .builder()
                    .arrival(scheduledServiceDto.getDirectRouteData().getAirportTo())
                    .departure(scheduledServiceDto.getDirectRouteData().getAirportFrom())
                    .year(scheduledServiceDto.getYearMonthData().getYear())
                    .month(scheduledServiceDto.getYearMonthData().getMonth())
                    .build();

            Mono<ScheduleAPIResponseModel> schedules = backendAPIService.getSchedules(scheduleAPIRequestModel);
            return schedules.flatMap(scheduleAPIResponseModel ->
                    filterAllAvailableFlightsFromScheduleResponse(
                            scheduledServiceDto.getRequestData(),
                            scheduleAPIResponseModel,
                            scheduledServiceDto.getDirectRouteData(),
                            scheduledServiceDto.getYearMonthData()
                    )
            );
    }

    /**
     * Filters available flights from the schedule response based on the provided criteria.
     *
     * @param requestDataDto    The DTO containing the request data.
     * @param scheduleAPIResponseModel The response model containing schedule data.
     * @param routeAPIResponseModel    The response model containing route data.
     * @param yearMonthDataDto  The DTO containing year and month data.
     * @return A Mono emitting FlightDataDto containing filtered flight data.
     */
    private Mono<FlightDataDto> filterAllAvailableFlightsFromScheduleResponse(RequestDataDto requestDataDto, ScheduleAPIResponseModel scheduleAPIResponseModel, RouteAPIResponseModel routeAPIResponseModel, YearMonthDataDto yearMonthDataDto) {
        List<Flight> selectedFlights = new ArrayList<>();
        int month = scheduleAPIResponseModel.getMonth();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_ISO);
        ArrayList<Day> days = scheduleAPIResponseModel.getDays();
        for (Day day : days) {
            List<Flight> flights = day.getFlights();
            for (Flight flight : flights) {
                String baseDateString = String.valueOf(yearMonthDataDto.getYear()).concat("-").concat(String.format("%02d", month)).concat("-").concat(String.format("%02d", day.getDay())).concat("T");

                String flightArrivalTimeString = baseDateString.concat(flight.getArrivalTime());
                String flightDepartureTimeString = baseDateString.concat(flight.getDepartureTime());

                LocalDateTime flightArrivalTime = LocalDateTime.parse(flightArrivalTimeString, dateTimeFormatter);
                LocalDateTime flightDepartureTime = LocalDateTime.parse(flightDepartureTimeString, dateTimeFormatter);

                if (flightDepartureTime.isAfter(requestDataDto.getDepartureDateTime()) && flightArrivalTime.isBefore(requestDataDto.getArrivalDateTime())) {
                    selectedFlights.add(new Flight(flight.getCarrierCode(), flight.getNumber(), flightDepartureTime.format(dateTimeFormatter), flightArrivalTime.format(dateTimeFormatter)));
                }

            }

        }
        return Mono.just(FlightDataDto.builder().arrival(routeAPIResponseModel.getAirportTo()).departure(routeAPIResponseModel.getAirportFrom()).selectedFlights(selectedFlights).build());
    }
}
