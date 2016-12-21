/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 package org.ejbca.ui.web;

/**
 * Used for parameter errors on web pageges.
 * 
 * @version $Id: ParameterException.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class ParameterException extends Exception {

    private static final long serialVersionUID = -8861455593395367960L;

    /**
     * @param message string to be displayed on the error page of the admin web GUI.
     */
    public ParameterException(String message) {
        super(message);
    }
}
