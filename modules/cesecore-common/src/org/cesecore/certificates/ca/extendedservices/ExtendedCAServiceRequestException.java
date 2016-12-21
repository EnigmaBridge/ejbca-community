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
package org.cesecore.certificates.ca.extendedservices;

/** Error processing the extended CA Service request
 * 
 * @version $Id: ExtendedCAServiceRequestException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class ExtendedCAServiceRequestException extends java.lang.Exception {
    
    private static final long serialVersionUID = -7017580940361778607L;

    /**
     * Creates a new instance of <code>ExtendedCAServiceRequestException</code> without detail message.
     */
    public ExtendedCAServiceRequestException() {
        super();
    }
        
    /**
     * Constructs an instance of <code>ExtendedCAServiceRequestException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ExtendedCAServiceRequestException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>ExtendedCAServiceRequestException</code> with the specified cause.
     * @param msg the detail message.
     */
    public ExtendedCAServiceRequestException(Exception e) {
        super(e);
    }
}
