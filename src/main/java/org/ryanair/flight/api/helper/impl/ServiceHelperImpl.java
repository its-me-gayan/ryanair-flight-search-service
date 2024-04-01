package org.ryanair.flight.api.helper.impl;

import org.ryanair.flight.api.dto.InterConnectedFlightData;
import org.ryanair.flight.api.dto.RequestDataDto;
import org.ryanair.flight.api.dto.SelectedFlightDataDto;
import org.ryanair.flight.api.dto.YearMonthDataDto;
import org.ryanair.flight.api.helper.ServiceHelper;
import org.ryanair.flight.api.model.Flight;
import org.ryanair.flight.api.util.Constant;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 4/1/24
 * Time: 12:44 PM
 */

/**
 * Implementation of ServiceHelper interface providing various helper methods for service layer.
 */
@Component
public class ServiceHelperImpl implements ServiceHelper {
    @Override
    public Flight findTwoHourAfterClosestFlightByGivenDateTime(LocalDateTime dateTime, List<Flight> flightList) {
        long minDifference = Long.MIN_VALUE;
        int index = -1;
        for (int i = 0; i < flightList.size(); i++) {
            Flight flight = flightList.get(i);
            LocalDateTime departLocalDateTime = LocalDateTime.parse(flight.getDepartureTime(), DateTimeFormatter.ofPattern(Constant.DATE_FORMAT_ISO));
            LocalDateTime plus2LocalDateTime = dateTime.plusHours(2);
            long diff = departLocalDateTime.until(plus2LocalDateTime, java.time.temporal.ChronoUnit.SECONDS);

            if (diff <= 0) {
                if (diff > minDifference) {
                    minDifference = diff;
                    index = i;
                }
            }
        }
        if (index == -1) {
            return null;
        }
        return flightList.get(index);
    }

    @Override
    public List<YearMonthDataDto> calculateNoOfMonthForTheProvidedDateRange(RequestDataDto requestDataDto) {
        List<YearMonthDataDto> yearAndMonth = new ArrayList<>();

        LocalDateTime localDateTimeDeparture = requestDataDto.getDepartureDateTime();
        LocalDateTime localDateTimeArrival = requestDataDto.getArrivalDateTime();

        int departureYear = localDateTimeDeparture.getYear();
        int arrivalYear = localDateTimeArrival.getYear();

        int departureMonth = localDateTimeDeparture.getMonthValue();
        int arrivalMonth = localDateTimeArrival.getMonthValue();

        if ((departureYear == arrivalYear) && (departureMonth == arrivalMonth)) {
            yearAndMonth.add(new YearMonthDataDto(departureYear, departureMonth));
        } else {
            LocalDateTime date = localDateTimeDeparture;
            if (date.getDayOfMonth() == 1) {
                date = date.minusDays(1);
            }
            while (date.isBefore(localDateTimeArrival)) {
                if (date.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).isAfter(localDateTimeArrival)) {
                    break;
                }
                date = date.plusMonths(1).withDayOfMonth(1);
                yearAndMonth.add(new YearMonthDataDto(date.getYear(), date.getMonthValue()));
            }
            yearAndMonth.add(new YearMonthDataDto(arrivalYear, arrivalMonth));
        }
        return yearAndMonth;
    }

    @Override
    public void linearizingDepartingAndArrivingInterconnectedFlights(HashMap<String, List<Flight>> departFlightDataMap, HashMap<String, List<Flight>> arriveFlightDataMap, List<InterConnectedFlightData> interConnectedFlightData) {

        for (InterConnectedFlightData connectedFlightData : interConnectedFlightData) {
            List<SelectedFlightDataDto> arriveFlightData = connectedFlightData.getArriveFlightData();
            List<SelectedFlightDataDto> departureFlightData = connectedFlightData.getDepartureFlightData();

            for (SelectedFlightDataDto selectedFlightDataDto : arriveFlightData) {
                String arrival = selectedFlightDataDto.getArrival();
                String departure = selectedFlightDataDto.getDeparture();

                String key = departure + "-" + arrival;
                if (Objects.isNull(arriveFlightDataMap.get(key))) {
                    arriveFlightDataMap.put(key, selectedFlightDataDto.getSelectedFlights());
                } else {
                    arriveFlightDataMap.get(key).addAll(selectedFlightDataDto.getSelectedFlights());
                }

            }

            for (SelectedFlightDataDto selectedFlightDataDto : departureFlightData) {
                String arrival = selectedFlightDataDto.getArrival();
                String departure = selectedFlightDataDto.getDeparture();

                String key = departure + "-" + arrival;

                if (Objects.isNull(departFlightDataMap.get(key))) {
                    departFlightDataMap.put(key, selectedFlightDataDto.getSelectedFlights());
                } else {
                    departFlightDataMap.get(key).addAll(selectedFlightDataDto.getSelectedFlights());
                }

            }
        }
    }

    @Override
    public void linearizingDepartingAndArrivingDirectFlights(HashMap<String, List<Flight>> linearDirectFlightMap, List<SelectedFlightDataDto> directFlightList) {

        for (SelectedFlightDataDto directFlightData : directFlightList) {
            String arrival = directFlightData.getArrival();
            String departure = directFlightData.getDeparture();
            String key = departure + "-" + arrival;

            List<Flight> existingFlights = linearDirectFlightMap.getOrDefault(key, new ArrayList<>());
            List<Flight> selectedFlights = directFlightData.getSelectedFlights();

            for (Flight flight : selectedFlights) {
                if (!existingFlights.contains(flight)) {
                    existingFlights.add(flight);
                }
            }

            linearDirectFlightMap.put(key, existingFlights);
        }
    }

}
