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
package org.cesecore.audit.enums;

/**
 * Represents the basic service types supported.
 *
 * When doing secure audit log ServiceType is used to indicate if the log  
 * was executed from the core itself or by an external application.
 * 
 * @see org.cesecore.audit.enums.EventTypes
 * @see org.cesecore.audit.enums.ModuleTypes
 * @version $Id: ServiceTypes.java 20951 2015-03-20 13:15:30Z jeklund $
 */
public enum ServiceTypes implements ServiceType {
    /** CE Security Core */
	CORE;

	@Override
    public boolean equals(ServiceType value) {
        if(value == null) {
            return false;
        }
        return this.toString().equals(value.toString());
    }
}
