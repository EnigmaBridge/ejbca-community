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
package org.cesecore.certificates.certificatetransparency;

import org.cesecore.CesecoreException;



/**
 * Failure to insert a certificate into Certificate Transparency logs
 *
 * @version $Id: CTLogException.java 18447 2014-02-05 16:13:00Z samuellb $
 */
public class CTLogException extends CesecoreException {

    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor used to create exception with an error message. Calls the same constructor in
     * baseclass <code>Exception</code>.
     *
     * @param message Human readable error message, can not be NULL.
     */
    public CTLogException(final String message) {
        super(message);
    }
    /**
     * Constructor used to create exception with an embedded exception. Calls the same constructor
     * in baseclass <code>Exception</code>.
     *
     * @param exception exception to be embedded.
     */
    public CTLogException(final Exception exception) {
        super(exception);
    }
}
