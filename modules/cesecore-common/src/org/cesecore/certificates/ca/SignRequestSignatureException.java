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
package org.cesecore.certificates.ca;

import org.cesecore.CesecoreException;

/**
 * Error due to invalid signature on certificate request.
 *
 * @version $Id: SignRequestSignatureException.java 19230 2014-06-23 08:23:36Z mikekushner $
 */
public class SignRequestSignatureException extends CesecoreException {

    private static final long serialVersionUID = -8038529907771953827L;

    /**
     * Constructor used to create exception with an error message. Calls the same constructor in
     * base class <code>Exception</code>.
     *
     * @param message Human readable error message, can not be NULL.
     */
    public SignRequestSignatureException(final String message) {
        super(message);
    }
    
    public SignRequestSignatureException(final String message, Throwable cause) {
        super(message, cause);
    }
}
