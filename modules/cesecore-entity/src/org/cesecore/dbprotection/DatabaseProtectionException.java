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
package org.cesecore.dbprotection;



/**
 * An exception thrown when there is an error with database protection, this error is fatal and should lead to 
 * application not working, thus the use of RuntimeException.
 *
 * @version $Id: DatabaseProtectionException.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class DatabaseProtectionException extends RuntimeException {
    
    private static final long serialVersionUID = -1L;

    /* We don't want to send raw database entries outside the JVM. */
    private transient final ProtectedData entity;

    /**
     * Constructs an instance of exception with a simple details message
     * and the read entity causing the error.
     */
    public DatabaseProtectionException(final String msg, final ProtectedData entity) {
        super(msg);
        this.entity = entity;
    }

    
    /**
     * Constructs an instance of exception with a simple details message
     * and the underlying exception
     */
    public DatabaseProtectionException(final String msg, final Exception e) {
        super(msg, e);
        this.entity = null;
    }
    /**
     * Constructs an instance of exception with a simple details message
     */
    public DatabaseProtectionException(final String msg) {
        super(msg);
        this.entity = null;
    }

    /**
     * Constructs an instance of exception wrapping the causing error
     */
    public DatabaseProtectionException(final Exception e) {
        super(e);
        this.entity = null;
    }
    
    /** @return the entity that we tried to read that failed the verification. */
    public ProtectedData getEntity() {
    	return entity;
    }
}
