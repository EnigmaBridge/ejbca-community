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
package org.cesecore.certificates.certificate.certextensions;

/**
 * Exception thrown of advanced certificate extensions when it is configured with bad properties.
 * 
 * @version $Id: CertificateExtensionException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class CertificateExtensionException extends Exception {

    private static final long serialVersionUID = 3664827690607121L;

    /**
     * Exception thrown of advanced certificate extensions when it is configured with bad properties.
     */
    public CertificateExtensionException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Exception thrown of advanced certificate extensions when it is configured with bad properties.
     */
    public CertificateExtensionException(String message) {
        super(message);
    }

}
