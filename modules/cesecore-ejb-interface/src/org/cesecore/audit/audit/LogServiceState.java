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

import java.util.concurrent.atomic.AtomicBoolean;

import org.cesecore.audit.log.AuditLogResetException;

/**
 * Keep the state of security audit logging. Enabled or disabled.
 * 
 * @version $Id: LogServiceState.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public enum LogServiceState {
	INSTANCE;

	private AtomicBoolean disabled = new AtomicBoolean(false);

	/** @return true if security audit logging has been disabled. */
	public boolean isDisabled() {
		return disabled.get();
	}
	
	/** Disable security audit logging. */
	protected void disable() throws AuditLogResetException {
		if (disabled.getAndSet(true)) {
			throw new AuditLogResetException("Cannot disable Security audit logging, since it was already disabled.");
		}
	}
	
	/** Enable security audit logging. */
	protected void enable() throws AuditLogResetException {
		if (!disabled.getAndSet(false)) {
			throw new AuditLogResetException("Cannot enable Security audit logging, since it was already enabled.");
		}
	}
}
