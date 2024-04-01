package org.ryanair.flight.api.dto;

import lombok.*;
import org.ryanair.flight.api.model.Flight;

import java.util.List;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/29/24
 * Time: 8:45 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SelectedFlightDataDto {
    private String departure;
    private String arrival;
    private List<Flight> selectedFlights;
}
