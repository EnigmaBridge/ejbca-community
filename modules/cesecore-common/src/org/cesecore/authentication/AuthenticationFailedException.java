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
package org.cesecore.authentication;

/**
 * This Exception is thrown whenever a public session bean is entered with an invalid authentication token.
 * 
 * @version $Id: AuthenticationFailedException.java 17625 2013-09-20 07:12:06Z netmackan $
 * 
 */
public class AuthenticationFailedException extends Exception {

    private static final long serialVersionUID = -9039667800941881965L;

    public AuthenticationFailedException() {
        super();
    }

    public AuthenticationFailedException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public AuthenticationFailedException(String arg0) {
        super(arg0);
    }

    public AuthenticationFailedException(Throwable arg0) {
        super(arg0);
    }

}
