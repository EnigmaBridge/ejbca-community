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
 
package org.ejbca.core.model.ca;

import org.ejbca.core.EjbcaException;



/**
 * Authentication error due to wrong credentials of user object. To authenticate a user the user
 * must have valid credentials, i.e. password.
 *
 * @version $Id: AuthLoginException.java 19901 2014-09-30 14:29:38Z anatom $
 */
public class AuthLoginException extends EjbcaException {
    private static final long serialVersionUID = -1950899421562556793L;

    /**
     * Constructor used to create exception with an errormessage. Calls the same constructor in
     * baseclass <code>Exception</code>.
     *
     * @param message Human redable error message, can not be NULL.
     */
    public AuthLoginException(String message) {
        super(message);
    }
}
