/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
/**
 * @version $Id: ResponseStatus.java 17625 2013-09-20 07:12:06Z netmackan $
 */
package org.cesecore.certificates.certificate.request;

import java.io.Serializable;

/**
 * Encapsulates the possible values for the status of a certificate response. Original response codes from the SCEP protocol.
 *
 * @version $Id: ResponseStatus.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public final class ResponseStatus implements Serializable {

    private static final long serialVersionUID = -1424581065308042345L;

    /**
     * Request granted
     */
    public static final ResponseStatus SUCCESS = new ResponseStatus(0);

    /**
     * Request granted with mods. Indicates the requester got something like what you asked for.
     * The requester is responsible for ascertaining the differences.
     */
    public static final ResponseStatus GRANTED_WITH_MODS = new ResponseStatus(1);
    
    /**
     * Request rejected
     */
    public static final ResponseStatus FAILURE = new ResponseStatus(2);

    /**
     * Request pending for approval
     */
    public static final ResponseStatus PENDING = new ResponseStatus(3);

    /**
     * The value actually encoded into the response message as a pkiStatus attribute
     */
    private final int value;

    private ResponseStatus(final int value) {
        this.value = value;
    }

    /**
     * Gets the value embedded in the response message as a pkiStatus attribute
     * @return  the value to use
     */
    public String getStringValue() {
        return Integer.toString(value);
    }

    public int getValue() {
    	return value;
    }

    public boolean equals(final Object o) {
    	boolean ret = false;
        if (this == o) {
        	ret = true;
        } else {
            if (o instanceof ResponseStatus) {
                final ResponseStatus status = (ResponseStatus) o;
                if (value == status.getValue()) {
                	ret = true;
                }
            }      	
        }
        return ret;
    }

    public int hashCode() {
        return value;
    }
}
