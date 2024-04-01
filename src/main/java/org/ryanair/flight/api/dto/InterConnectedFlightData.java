package org.ryanair.flight.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/29/24
 * Time: 11:49 PM
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class InterConnectedFlightData {
    private List<SelectedFlightDataDto> departureFlightData;
    private List<SelectedFlightDataDto> arriveFlightData;
}
