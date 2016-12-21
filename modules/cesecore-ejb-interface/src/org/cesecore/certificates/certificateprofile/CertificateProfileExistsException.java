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

package org.cesecore.certificates.certificateprofile;

/**
 * An exception thrown when someone tries to add a certificate profile that already exits
 * 
 * @version $Id: CertificateProfileExistsException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class CertificateProfileExistsException extends Exception {

    private static final long serialVersionUID = 1155162706774947712L;

    /**
     * Creates a new instance of <code>CertificateProfileExistsException</code> without detail message.
     */
    public CertificateProfileExistsException() {
        super();
    }

    /**
     * Constructs an instance of <code>CertificateProfileExistsException</code> with the specified detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public CertificateProfileExistsException(String msg) {
        super(msg);
    }
}
