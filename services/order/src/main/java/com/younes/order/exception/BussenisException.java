package com.younes.order.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BussenisException
 */
@Data 
@EqualsAndHashCode (callSuper = true)
public class BussenisException  extends RuntimeException{

    private final String msg;

}
