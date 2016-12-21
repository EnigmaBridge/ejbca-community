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

import javax.ejb.ApplicationException;

/**
 * Handles secure audit log storage exceptions
 * 
 * @version $Id: AuditRecordStorageException.java 17625 2013-09-20 07:12:06Z netmackan $
 * 
 */
@ApplicationException(rollback=true)
public class AuditRecordStorageException extends RuntimeException {

    private static final long serialVersionUID = -2049206241984967597L;

    public AuditRecordStorageException() {
        super();
    }

    public AuditRecordStorageException(String message) {
        super(message);
    }

    public AuditRecordStorageException(Throwable t) {
        super(t);
    }

    public AuditRecordStorageException(String s, Throwable t) {
        super(s, t);
    }
}
