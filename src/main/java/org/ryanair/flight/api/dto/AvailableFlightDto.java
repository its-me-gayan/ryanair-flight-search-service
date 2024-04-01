package org.ryanair.flight.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/31/24
 * Time: 12:51 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableFlightDto {
    private List<SelectedFlightDataDto> directFlights;
    private List<InterConnectedFlightData> interconnectedFlights;
}
