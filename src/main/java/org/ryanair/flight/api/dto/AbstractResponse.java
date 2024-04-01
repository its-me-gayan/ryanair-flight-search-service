package org.ryanair.flight.api.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Author: Gayan Sanjeewa
 * User: gayan
 * Date: 3/31/24
 * Time: 3:35 PM
 */
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AbstractResponse implements Serializable {
    private String timeStamp;
    private int responseCode;
    private String message;
    private String messageDescription;
    private Object data;
}
