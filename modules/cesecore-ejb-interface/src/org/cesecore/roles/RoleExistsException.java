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
package org.cesecore.roles;

/**
 * @version $Id: RoleExistsException.java 17625 2013-09-20 07:12:06Z netmackan $
 *
 */
public class RoleExistsException extends Exception {


    private static final long serialVersionUID = -8498629513600190809L;

    /**
     * 
     */
    public RoleExistsException() {
    }

    /**
     * @param message
     */
    public RoleExistsException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public RoleExistsException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RoleExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}
