package org.ryanair.flight.api.util;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/29/24
 * Time: 6:36 PM
 */
public record Constant() {
    public static final String DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm";
    public static final String ROUTE_TYPE_DIRECT = "DIRECT";
    public static final String ROUTE_TYPE_ONE_STOP = "ONE_STOP";
    public static final String PROVIDER = "RYANAIR";
    public static final String RESPONSE_MESSAGE_SUCCESS = "Data retrieved successfully";
    public static final String RESPONSE_MESSAGE_NO_CONTENT = "No any related flights Found for the the given criteria";
    public static final String RESPONSE_MESSAGE_FAILED = "Data retrieved Failed";
    public static final String RESPONSE_DESCRIPTION_INFO = " With %s Direct flights and %s Interconnect flights with one stop for the given criteria";

}
