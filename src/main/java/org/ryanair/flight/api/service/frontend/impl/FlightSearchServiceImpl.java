package org.ryanair.flight.api.service.frontend.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ryanair.flight.api.dto.*;
import org.ryanair.flight.api.exception.BackendInvocationException;
import org.ryanair.flight.api.helper.ServiceHelper;
import org.ryanair.flight.api.model.*;
import org.ryanair.flight.api.service.backend.BackendAPIService;
import org.ryanair.flight.api.service.frontend.FlightSearchService;
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
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class FlightSearchServiceImpl implements FlightSearchService {

    private final BackendAPIService backendAPIService;
    private final ServiceHelper serviceHelper;

    @Override
    public Mono<List<FinalFlightResponseDto>> findAllAvailableFlights(RequestDataDto requestDataDto) {

        String arrival = requestDataDto.getArrival();
        String departure = requestDataDto.getDeparture();

        Mono<List<RouteAPIResponseModel>> validRoutes = backendAPIService.getRoutes(arrival, departure);

        Mono<List<PossibleRoutesDto>> allPossibleRoutes = validRoutes.flatMap(routeAPIResponseModelList -> findAllPossibleRoutes(routeAPIResponseModelList, departure, arrival));

        Mono<AvailableFlightDto> allAvailableFlights = allPossibleRoutes.flatMap(routeAPIResponseModels -> findAllAvailableInterconnectedAndDirectFlights(routeAPIResponseModels, requestDataDto));

        return allAvailableFlights.flatMap(availableFlightDto -> processCollectedFlightsToFinalResponse(availableFlightDto, requestDataDto));

    }


    private Mono<List<FinalFlightResponseDto>> processCollectedFlightsToFinalResponse(AvailableFlightDto availableFlightDto, RequestDataDto requestDataDto) {


        List<FinalFlightResponseDto> finalFlightResponseDtoList = new ArrayList<>();

        //generating final response for the direct flights
        List<SelectedFlightDataDto> allAvailableDirectFlights = availableFlightDto.getDirectFlights();
        generateAndAttachDirectFlightsToFinaResponse(finalFlightResponseDtoList, allAvailableDirectFlights);


        //generating final response for the interconnected flights with checking conditions
        List<InterConnectedFlightData> allFoundedInterconnectedFlights = availableFlightDto.getInterconnectedFlights();
        generateAndAttachInterConnectedFlightsToFinaResponse(finalFlightResponseDtoList, allFoundedInterconnectedFlights, requestDataDto);


        return Mono.just(finalFlightResponseDtoList);
    }

    private void generateAndAttachInterConnectedFlightsToFinaResponse(List<FinalFlightResponseDto> finalFlightResponseDtoList, List<InterConnectedFlightData> allFoundedInterconnectedFlights, RequestDataDto requestDataDto) {
        if (!CollectionUtils.isEmpty(allFoundedInterconnectedFlights)) {
            HashMap<String, List<Flight>> arrivingFlightsDataMap = new HashMap<>();
            HashMap<String, List<Flight>> departingFlightsDataMap = new HashMap<>();

            serviceHelper.linearizingDepartingAndArrivingInterconnectedFlights(departingFlightsDataMap, arrivingFlightsDataMap, allFoundedInterconnectedFlights);

            findAndMapRelatedInterConnectedFlights(arrivingFlightsDataMap, departingFlightsDataMap, requestDataDto, finalFlightResponseDtoList);
        }
    }

    private void generateAndAttachDirectFlightsToFinaResponse(List<FinalFlightResponseDto> finalFlightResponseDtoList, List<SelectedFlightDataDto> directFlights) {
        if (!CollectionUtils.isEmpty(directFlights)) {
            HashMap<String, List<Flight>> directFlightMap = new HashMap<>();

            serviceHelper.linearizingDepartingAndArrivingDirectFlights(directFlightMap, directFlights);

            directFlightMap.forEach((s, flightList) -> {
                String[] splitAirport = s.split("-");
                flightList.forEach(flight -> finalFlightResponseDtoList.add(FinalFlightResponseDto.builder().stops(0).legs(Collections.singletonList(DataLegs.builder().departureDateTime(flight.getDepartureTime()).arrivalDateTime(flight.getArrivalTime()).departureAirport(splitAirport[0]).arrivalAirport(splitAirport[1]).build())).build()));
            });
        }
    }

    private void findAndMapRelatedInterConnectedFlights(HashMap<String, List<Flight>> arrivingFlightsDataMap, HashMap<String, List<Flight>> departingFlightsDataMap, RequestDataDto requestDataDto, List<FinalFlightResponseDto> finalFlightResponseDtoList) {
        departingFlightsDataMap.forEach((key, departingFlights) -> {
            String[] split = key.split("-");
            String splitDepartingAirport = split[0];
            String splitArrivingAirport = split[1];

            departingFlights.forEach(departingFlight -> {
                List<Flight> arrivingFlights = arrivingFlightsDataMap.get(split[1] + "-" + requestDataDto.getArrival());
                arrivingFlights.forEach(arrivingFlight -> {
                    LocalDateTime flightArrivingDateTime = LocalDateTime.parse(departingFlight.getArrivalTime(), DateTimeFormatter.ofPattern(DATE_FORMAT_ISO));
                    Flight closestFoundedArrivingFlight = serviceHelper.findTwoHourAfterClosestFlightByGivenDateTime(flightArrivingDateTime, arrivingFlights);
                    if (Objects.nonNull(departingFlight) && Objects.nonNull(closestFoundedArrivingFlight)) {

                        List<DataLegs> legs = new ArrayList<>();

                        legs.add(DataLegs.builder().departureAirport(requestDataDto.getDeparture()).arrivalAirport(splitArrivingAirport).arrivalDateTime(departingFlight.getArrivalTime()).departureDateTime(departingFlight.getDepartureTime()).build());

                        legs.add(DataLegs.builder().departureAirport(splitDepartingAirport).arrivalAirport(requestDataDto.getArrival()).arrivalDateTime(closestFoundedArrivingFlight.getArrivalTime()).departureDateTime(closestFoundedArrivingFlight.getDepartureTime()).build());

                        finalFlightResponseDtoList.add(FinalFlightResponseDto.builder().stops(1).legs(legs).build());
                    }
                });
            });
        });
    }


    public Mono<AvailableFlightDto> findAllAvailableInterconnectedAndDirectFlights(List<PossibleRoutesDto> allPossibleRoute, RequestDataDto requestDataDto) throws BackendInvocationException {

        Optional<PossibleRoutesDto> directRoutFirst = allPossibleRoute.stream().filter(possibleRoutesDto -> possibleRoutesDto.getType().equals(Constant.ROUTE_TYPE_DIRECT)).findFirst();

        Flux<InterConnectedFlightData> selectedInterconnectedFLightDataFlux = Flux.empty();
        Mono<List<SelectedFlightDataDto>> selectedDirectFlightListMono = Mono.empty();

        List<YearMonthDataDto> noOfMonthWithYear = serviceHelper.calculateNoOfMonthForTheProvidedDateRange(requestDataDto);

        System.out.println("processing route - " + Constant.ROUTE_TYPE_DIRECT);
        if (directRoutFirst.isPresent()) {
            RouteAPIResponseModel directRout = directRoutFirst.get().getRouteDetails().getFirst().getFirst();
            selectedDirectFlightListMono = getAvailableFlightForTheDirectRouteMono(directRout, noOfMonthWithYear, requestDataDto);
        } else {
            System.out.println("no direct route found");
        }

        System.out.println("processing routes - " + Constant.ROUTE_TYPE_ONE_STOP);

        Optional<PossibleRoutesDto> oneStopRoutes = allPossibleRoute.stream().filter(possibleRoutesDto -> possibleRoutesDto.getType().equals(Constant.ROUTE_TYPE_ONE_STOP)).findFirst();
        if (oneStopRoutes.isPresent()) {
            selectedInterconnectedFLightDataFlux = getInterConnectedAvailableFlightFlux(requestDataDto, oneStopRoutes, noOfMonthWithYear);
        } else {
            System.out.println("no inter connected flight founds");
        }
        return selectedDirectFlightListMono.defaultIfEmpty(Collections.emptyList()).zipWith(selectedInterconnectedFLightDataFlux.collectList().defaultIfEmpty(Collections.emptyList())).map(tuple -> {
            List<SelectedFlightDataDto> selectedDirectFlights = tuple.getT1();
            List<InterConnectedFlightData> interConnectedFlightData = tuple.getT2();

            // Create a new object to hold both types of data
            AvailableFlightDto combinedDetails = new AvailableFlightDto();
            combinedDetails.setDirectFlights(selectedDirectFlights);
            combinedDetails.setInterconnectedFlights(interConnectedFlightData);
            return combinedDetails;
        });

    }

    private Flux<InterConnectedFlightData> getInterConnectedAvailableFlightFlux(RequestDataDto requestDataDto, Optional<PossibleRoutesDto> oneStopRoutes, List<YearMonthDataDto> noOfMonthWithYear) {
        Flux<InterConnectedFlightData> selectedInterconnectedFLightDataFlux;
        List<List<RouteAPIResponseModel>> oneStopRoutesList = oneStopRoutes.get().getRouteDetails();

        System.out.println("oneStopRoutesList " + oneStopRoutesList.size());
        oneStopRoutesList.forEach(routeAPIResponseModelList -> System.out.println(routeAPIResponseModelList));
        selectedInterconnectedFLightDataFlux = Flux.fromIterable(oneStopRoutesList).flatMap(routeAPIResponseModelList -> {
            System.out.println("inside flux interate - " + routeAPIResponseModelList.size());
            routeAPIResponseModelList.forEach(routeAPIResponseModel -> System.out.println("inside " + routeAPIResponseModel));
            return Mono.just(routeAPIResponseModelList);
        }).flatMap(routeAPIResponseModels -> Flux.fromIterable(noOfMonthWithYear).flatMap(yearMonthDataDto -> {
                    RouteAPIResponseModel departingModel = routeAPIResponseModels.getFirst(); // Departing section
                    RouteAPIResponseModel arrivingModel = routeAPIResponseModels.getLast(); // Arriving section

                    ScheduleAPIRequestModel departingBuild = ScheduleAPIRequestModel.builder().arrival(departingModel.getAirportTo()).departure(departingModel.getAirportFrom()).year(yearMonthDataDto.getYear()).month(yearMonthDataDto.getMonth()).build();

                    ScheduleAPIRequestModel arrivingBuild = ScheduleAPIRequestModel.builder().arrival(arrivingModel.getAirportTo()).departure(arrivingModel.getAirportFrom()).year(yearMonthDataDto.getYear()).month(yearMonthDataDto.getMonth()).build();

                    Mono<ScheduleAPIResponseModel> departingSchedulesMono = backendAPIService.getSchedules(departingBuild);
                    Mono<ScheduleAPIResponseModel> arrivingSchedulesMono = backendAPIService.getSchedules(arrivingBuild);


                    Mono<SelectedFlightDataDto> selectedDepartingFlightDataMono = departingSchedulesMono.flatMap(scheduleAPIResponseModel -> filterAllAvailableFlightsFromScheduleResponse(requestDataDto, scheduleAPIResponseModel, departingModel, yearMonthDataDto));

                    Mono<SelectedFlightDataDto> selectedArrivingFlightDataMono = arrivingSchedulesMono.flatMap(scheduleAPIResponseModel -> filterAllAvailableFlightsFromScheduleResponse(requestDataDto, scheduleAPIResponseModel, arrivingModel, yearMonthDataDto));

                    return Mono.zip(selectedDepartingFlightDataMono, selectedArrivingFlightDataMono).map(objects -> {
                        InterConnectedFlightData interConnectedFlightData = new InterConnectedFlightData();
                        List<SelectedFlightDataDto> departureFlightData = new ArrayList<>();
                        List<SelectedFlightDataDto> arriveFlightData = new ArrayList<>();

                        departureFlightData.add(objects.getT1());
                        arriveFlightData.add(objects.getT2());

                        interConnectedFlightData.setDepartureFlightData(departureFlightData);
                        interConnectedFlightData.setArriveFlightData(arriveFlightData);
                        return interConnectedFlightData;
                    });
                })

        );
        return selectedInterconnectedFLightDataFlux;
    }

    private Mono<List<SelectedFlightDataDto>> getAvailableFlightForTheDirectRouteMono(RouteAPIResponseModel directRout, List<YearMonthDataDto> noOfMonthWithYear, RequestDataDto requestDataDto) {

        return Flux.fromIterable(noOfMonthWithYear).flatMap(yearMonthDataDto -> {
            ScheduleAPIRequestModel scheduleAPIRequestModel = ScheduleAPIRequestModel.builder().arrival(directRout.getAirportTo()).departure(directRout.getAirportFrom()).year(yearMonthDataDto.getYear()).month(yearMonthDataDto.getMonth()).build();

            Mono<ScheduleAPIResponseModel> schedules = backendAPIService.getSchedules(scheduleAPIRequestModel);
            return schedules.flatMap(scheduleAPIResponseModel -> filterAllAvailableFlightsFromScheduleResponse(requestDataDto, scheduleAPIResponseModel, directRout, yearMonthDataDto));
        }).collectList();
    }

    private Mono<SelectedFlightDataDto> filterAllAvailableFlightsFromScheduleResponse(RequestDataDto requestDataDto, ScheduleAPIResponseModel scheduleAPIResponseModel, RouteAPIResponseModel routeAPIResponseModel, YearMonthDataDto yearMonthDataDto) {
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
        return Mono.just(SelectedFlightDataDto.builder().arrival(routeAPIResponseModel.getAirportTo()).departure(routeAPIResponseModel.getAirportFrom()).selectedFlights(selectedFlights).build());
    }


    public Mono<List<PossibleRoutesDto>> findAllPossibleRoutes(List<RouteAPIResponseModel> list, String departureIATACode, String arrivalIATACode) {
        List<PossibleRoutesDto> possibleRoutesDtoList = new ArrayList<>();
        // finding direct route
        List<RouteAPIResponseModel> directRoute = list.stream().filter(routeAPIResponseModel -> routeAPIResponseModel.getAirportFrom().equals(departureIATACode) && routeAPIResponseModel.getAirportTo().equals(arrivalIATACode)).toList();

        if (!directRoute.isEmpty()) {
            PossibleRoutesDto possibleRoutesDto = new PossibleRoutesDto();
            possibleRoutesDto.setType(Constant.ROUTE_TYPE_DIRECT);
            possibleRoutesDto.setRouteDetails(Collections.singletonList(directRoute));
            possibleRoutesDtoList.add(possibleRoutesDto);
        }

        //other possible routes with one stop
        for (int i = 0; i < list.size(); i++) {
            RouteAPIResponseModel model = list.get(i);
            String airportTo = model.getAirportTo();
            if (departureIATACode.equals(model.getAirportFrom())) {
                for (int j = 0; j < list.size(); j++) {
                    RouteAPIResponseModel mm = list.get(j);
                    if (airportTo.equals(mm.getAirportFrom())) {
                        if (mm.getAirportTo().equals(arrivalIATACode)) {
                            PossibleRoutesDto possibleRoutesDto = new PossibleRoutesDto();
                            possibleRoutesDto.setType(Constant.ROUTE_TYPE_ONE_STOP);
                            possibleRoutesDto.setRouteDetails(Collections.singletonList(Arrays.asList(model, mm)));
                            possibleRoutesDtoList.add(possibleRoutesDto);
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("possibleRoutesList " + possibleRoutesDtoList);
        return Mono.just(possibleRoutesDtoList);
    }

}
