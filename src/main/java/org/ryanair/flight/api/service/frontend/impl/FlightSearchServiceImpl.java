package org.ryanair.flight.api.service.frontend.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ryanair.flight.api.dto.*;
import org.ryanair.flight.api.exception.BackendInvocationException;
import org.ryanair.flight.api.helper.ServiceHelper;
import org.ryanair.flight.api.model.*;
import org.ryanair.flight.api.service.frontend.FlightSearchService;
import org.ryanair.flight.api.service.frontend.RouteService;
import org.ryanair.flight.api.service.frontend.ScheduleService;
import org.ryanair.flight.api.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.ryanair.flight.api.util.Constant.DATE_FORMAT_ISO;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/28/24
 * Time: 4:17 PM
 */

/**
 * Implementation of FlightSearchService that finds all available flights based on the given criteria.
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class FlightSearchServiceImpl implements FlightSearchService {

    private final ServiceHelper serviceHelper;
    private final RouteService routeService;
    private final ScheduleService scheduleService;

    /**
     * Finds all available flights based on the given request data.
     * @param requestDataDto The request data containing departure and arrival details.
     * @return A Mono emitting a list of FinalFlightResponseDto objects.
     */
    @Override
    public Mono<List<FinalFlightResponseDto>> findAllAvailableFlights(RequestDataDto requestDataDto) {

        String arrival = requestDataDto.getArrival();
        String departure = requestDataDto.getDeparture();

        return routeService.findAllPossibleRoutes(departure, arrival) //finding all possible routes
                .flatMap(routeAPIResponseModels ->
                        processAllAvailableInterconnectedAndDirectFlights(routeAPIResponseModels, requestDataDto)) // finding all available flights
                .flatMap(availableFlightDto ->
                        processCollectedFlightsToFinalResponse(availableFlightDto, requestDataDto)); //combining all connected flights together

    }

    /**
     * Processes collected flights to generate final flight response.
     * @param availableFlightDto The available flight data.
     * @param requestDataDto The request data.
     * @return A Mono emitting a list of FinalFlightResponseDto objects.
     */
    private Mono<List<FinalFlightResponseDto>> processCollectedFlightsToFinalResponse(AvailableFlightDto availableFlightDto, RequestDataDto requestDataDto) {

        List<FinalFlightResponseDto> finalFlightResponseDtoList = new ArrayList<>();

        //generating final response for the direct flights
        List<FlightDataDto> allAvailableDirectFlights = availableFlightDto.getDirectFlights();
        generateAndAttachDirectFlightsToFinaResponse(finalFlightResponseDtoList, allAvailableDirectFlights);

        //generating final response for the interconnected flights with checking conditions
        List<InterConnectedFlightData> allFoundedInterconnectedFlights = availableFlightDto.getInterconnectedFlights();
        generateAndAttachInterConnectedFlightsToFinaResponse(
                finalFlightResponseDtoList, allFoundedInterconnectedFlights, requestDataDto);


        return Mono.just(finalFlightResponseDtoList);
    }

    /**
     * Generates and attaches interconnected flights to the final flight response.
     * @param finalFlightResponseDtoList The list to which final flight response DTOs are added.
     * @param allFoundedInterconnectedFlights List of interconnected flight data.
     * @param requestDataDto The request data.
     */
    private void generateAndAttachInterConnectedFlightsToFinaResponse(List<FinalFlightResponseDto> finalFlightResponseDtoList, List<InterConnectedFlightData> allFoundedInterconnectedFlights, RequestDataDto requestDataDto) {
        if (!CollectionUtils.isEmpty(allFoundedInterconnectedFlights)) {
            HashMap<String, List<Flight>> arrivingFlightsDataMap = new HashMap<>();
            HashMap<String, List<Flight>> departingFlightsDataMap = new HashMap<>();

            serviceHelper.linearizingDepartingAndArrivingInterconnectedFlights(
                    departingFlightsDataMap, arrivingFlightsDataMap, allFoundedInterconnectedFlights);

            findAndMapRelatedInterConnectedFlights(
                    arrivingFlightsDataMap, departingFlightsDataMap, requestDataDto, finalFlightResponseDtoList);
        }
    }

    /**
     * Generates and attaches direct flights to the final flight response.
     * @param finalFlightResponseDtoList The list to which final flight response DTOs are added.
     * @param directFlights List of direct flight data.
     */
    private void generateAndAttachDirectFlightsToFinaResponse(List<FinalFlightResponseDto> finalFlightResponseDtoList, List<FlightDataDto> directFlights) {
        if (!CollectionUtils.isEmpty(directFlights)) {
            HashMap<String, List<Flight>> directFlightMap = new HashMap<>();

            serviceHelper.linearizingDepartingAndArrivingDirectFlights(directFlightMap, directFlights);

            directFlightMap.forEach((s, flightList) -> {
                String[] splitAirport = s.split("-");
                flightList.forEach(flight ->
                        finalFlightResponseDtoList.add(
                                FinalFlightResponseDto.builder()
                                        .stops(0)
                                        .legs(
                                                Collections.singletonList(
                                                DataLegs.builder()
                                                        .departureDateTime(flight.getDepartureTime())
                                                        .arrivalDateTime(flight.getArrivalTime())
                                                        .departureAirport(splitAirport[0])
                                                        .arrivalAirport(splitAirport[1])
                                                        .build()))
                                        .build()
                        )
                );
            });
        }
    }


    /**
     * Finds and maps related interconnected flights to the final flight response.
     * @param arrivingFlightsDataMap Map containing arriving flights data.
     * @param departingFlightsDataMap Map containing departing flights data.
     * @param requestDataDto The request data.
     * @param finalFlightResponseDtoList The list to which final flight response DTOs are added.
     */
    private void findAndMapRelatedInterConnectedFlights(HashMap<String, List<Flight>> arrivingFlightsDataMap, HashMap<String, List<Flight>> departingFlightsDataMap, RequestDataDto requestDataDto, List<FinalFlightResponseDto> finalFlightResponseDtoList) {
        departingFlightsDataMap.forEach((key, departingFlights) -> {
            String[] split = key.split("-");
            String splitDepartingAirport = split[0];
            String splitArrivingAirport = split[1];
            departingFlights.forEach(departingFlight -> {
                List<Flight> arrivingFlights = arrivingFlightsDataMap.get(split[1] + "-" + requestDataDto.getArrival());
                arrivingFlights.forEach(arrivingFlight -> {

                    LocalDateTime flightArrivingDateTime = LocalDateTime
                            .parse(departingFlight.getArrivalTime(), DateTimeFormatter.ofPattern(DATE_FORMAT_ISO));

                    Flight closestFoundedArrivingFlight = serviceHelper
                            .findTwoHourAfterClosestFlightByGivenDateTime(flightArrivingDateTime, arrivingFlights);

                    if (Objects.nonNull(closestFoundedArrivingFlight)) {
                        List<DataLegs> legs = new ArrayList<>();
                        legs.add(
                                DataLegs.builder()
                                        .departureAirport(requestDataDto.getDeparture())
                                        .arrivalAirport(splitArrivingAirport)
                                        .arrivalDateTime(departingFlight.getArrivalTime())
                                        .departureDateTime(departingFlight.getDepartureTime())
                                        .build()
                        );

                        legs.add(
                                DataLegs.builder()
                                        .departureAirport(splitDepartingAirport)
                                        .arrivalAirport(requestDataDto.getArrival())
                                        .arrivalDateTime(closestFoundedArrivingFlight.getArrivalTime())
                                        .departureDateTime(closestFoundedArrivingFlight.getDepartureTime())
                                        .build()
                        );
                        finalFlightResponseDtoList.add(FinalFlightResponseDto.builder().stops(1).legs(legs).build());
                    }
                });
            });
        });
    }


    /**
     * Process all available interconnected and direct flights based on the given possible routes and request data.
     * @param allPossibleRoute List of possible routes.
     * @param requestDataDto The request data.
     * @return A Mono emitting the available flight data.
     * @throws BackendInvocationException if an error occurs during backend invocation.
     */
    public Mono<AvailableFlightDto> processAllAvailableInterconnectedAndDirectFlights(List<PossibleRoutesDto> allPossibleRoute, RequestDataDto requestDataDto) throws BackendInvocationException {

        //filtering and get direct route from the allPossibleRoute list
        Optional<PossibleRoutesDto> directRoutFirst = allPossibleRoute
                .stream()
                .filter(possibleRoutesDto ->
                        possibleRoutesDto.getType().equals(Constant.ROUTE_TYPE_DIRECT))
                .findFirst();

        Flux<InterConnectedFlightData> selectedInterconnectedFLightDataFlux = Flux.empty();
        Mono<List<FlightDataDto>> selectedDirectFlightListMono = Mono.empty();

        List<YearMonthDataDto> noOfMonthWithYear = serviceHelper.calculateNoOfMonthForTheProvidedDateRange(requestDataDto);

        System.out.println("processing route - " + Constant.ROUTE_TYPE_DIRECT);
        if (directRoutFirst.isPresent()) {
            RouteAPIResponseModel directRout = directRoutFirst.get().getRouteDetails().getFirst().getFirst();
            selectedDirectFlightListMono = getAvailableFlightForTheDirectRouteMono(directRout, noOfMonthWithYear, requestDataDto);
        } else {
            System.out.println("no direct route found");
        }

        System.out.println("processing routes - " + Constant.ROUTE_TYPE_ONE_STOP);

        //filtering and get interconnected routes from the allPossibleRoute list
        Optional<PossibleRoutesDto> oneStopRoutes = allPossibleRoute
                .stream()
                .filter(possibleRoutesDto ->
                        possibleRoutesDto.getType().equals(Constant.ROUTE_TYPE_ONE_STOP))
                .findFirst();

        if (oneStopRoutes.isPresent()) {
            selectedInterconnectedFLightDataFlux = getInterConnectedAvailableFlightFlux(requestDataDto, oneStopRoutes.get(), noOfMonthWithYear);
        } else {
            System.out.println("no inter connected flight founds");
        }

        //combining direct flights and interconnected flights mono's together
        return selectedDirectFlightListMono.defaultIfEmpty(Collections.emptyList())
                .zipWith(selectedInterconnectedFLightDataFlux.collectList().defaultIfEmpty(Collections.emptyList()))
                .map(tuple -> {
            List<FlightDataDto> selectedDirectFlights = tuple.getT1();
            List<InterConnectedFlightData> interConnectedFlightData = tuple.getT2();

            // Create a new object to hold both types of data
            AvailableFlightDto combinedDetails = new AvailableFlightDto();
            combinedDetails.setDirectFlights(selectedDirectFlights);
            combinedDetails.setInterconnectedFlights(interConnectedFlightData);
            return combinedDetails;
        });

    }

    /**
     * Retrieves the flux of available interconnected flights.
     * @param requestDataDto The request data.
     * @param interConnectedRoutes Details of interconnected routes.
     * @param noOfMonthWithYear The list of YearMonthDataDto objects.
     * @return Flux emitting InterConnectedFlightData.
     */
    private Flux<InterConnectedFlightData> getInterConnectedAvailableFlightFlux(RequestDataDto requestDataDto, PossibleRoutesDto interConnectedRoutes, List<YearMonthDataDto> noOfMonthWithYear) {
        List<List<RouteAPIResponseModel>> oneStopRoutesList = interConnectedRoutes.getRouteDetails();
        return Flux.fromIterable(oneStopRoutesList)
                .flatMap(routeAPIResponseModels -> Flux.fromIterable(noOfMonthWithYear)
                        .flatMap(yearMonthDataDto -> {
            ScheduledServiceDto scheduledServiceDto = ScheduledServiceDto.builder()
                    .arrivingRouteData(routeAPIResponseModels.getLast()) // Arriving section
                    .departingRouteData(routeAPIResponseModels.getFirst()) // Departing section
                    .requestData(requestDataDto)
                    .yearMonthData(yearMonthDataDto)
                    .build();

            Mono<FlightDataDto> scheduledDepartingFlightData = scheduleService.getScheduledDepartingFlightData(scheduledServiceDto);
            Mono<FlightDataDto> scheduledArrivingFlightData = scheduleService.getScheduledArrivingFlightData(scheduledServiceDto);
            return Mono.zip(scheduledDepartingFlightData, scheduledArrivingFlightData).map(objects -> {
                        InterConnectedFlightData interConnectedFlightData = new InterConnectedFlightData();
                        List<FlightDataDto> departureFlightData = new ArrayList<>();
                        List<FlightDataDto> arriveFlightData = new ArrayList<>();

                        departureFlightData.add(objects.getT1());
                        arriveFlightData.add(objects.getT2());

                        interConnectedFlightData.setDepartureFlightData(departureFlightData);
                        interConnectedFlightData.setArriveFlightData(arriveFlightData);
                        return interConnectedFlightData;
                    });
                })

        );
    }


    /**
     * Retrieves the mono of available direct flights.
     * @param directRoute Details of the direct route.
     * @param noOfMonthWithYear The list of YearMonthDataDto objects.
     * @param requestDataDto The request data.
     * @return Mono emitting a list of FlightDataDto objects.
     */
    private Mono<List<FlightDataDto>> getAvailableFlightForTheDirectRouteMono(
            RouteAPIResponseModel directRoute,
            List<YearMonthDataDto> noOfMonthWithYear,
            RequestDataDto requestDataDto)
    {
        return Flux.fromIterable(noOfMonthWithYear).flatMap(yearMonthDataDto ->
                scheduleService.getScheduledDirectFlightData(ScheduledServiceDto.builder()
                .directRouteData(directRoute) // direct route section
                .requestData(requestDataDto)
                .yearMonthData(yearMonthDataDto)
                .build())).collectList();
    }
}
