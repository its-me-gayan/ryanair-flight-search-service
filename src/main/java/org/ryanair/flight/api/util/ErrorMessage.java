package org.ryanair.flight.api.util;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/31/24
 * Time: 9:06 PM
 */
public record ErrorMessage() {
    public static final String ERR_MSG_NO_VALID_ROUTE_FOUND ="No Valid Route found for given IATA combination";
    public static final String ERR_INVALID_REQ_PARAMETERS ="Invalid request parameters";
}
