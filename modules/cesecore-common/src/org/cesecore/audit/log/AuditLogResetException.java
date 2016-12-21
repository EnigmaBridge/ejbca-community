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
package org.cesecore.audit.log;

/**
 * @version $Id: AuditLogResetException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class AuditLogResetException extends Exception {

	private static final long serialVersionUID = 1L;

	public AuditLogResetException() {
        super();
    }

    public AuditLogResetException(final String message) {
        super(message);
    }

    public AuditLogResetException(final Throwable t) {
        super(t);
    }

    public AuditLogResetException(final String s, final Throwable t) {
        super(s, t);
    }
}
