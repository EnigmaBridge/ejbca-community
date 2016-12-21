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
package org.cesecore.audit.audit;

/**
 * An error during validation of secure audit log data.
 * 
 * @version $Id: AuditLogValidatorException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class AuditLogValidatorException extends Exception{

    private static final long serialVersionUID = 1L;

    public AuditLogValidatorException() {
        super();
    }

    public AuditLogValidatorException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public AuditLogValidatorException(final String message) {
        super(message);
    }

    public AuditLogValidatorException(final Throwable throwable) {
        super(throwable);
    }
}
