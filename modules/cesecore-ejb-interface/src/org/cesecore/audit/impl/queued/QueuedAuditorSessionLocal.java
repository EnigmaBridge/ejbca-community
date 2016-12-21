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
package org.cesecore.audit.impl.queued;

import java.util.Date;

import javax.ejb.Local;

import org.cesecore.audit.Auditable;
import org.cesecore.audit.log.AuditLogResetException;
import org.cesecore.authentication.tokens.AuthenticationToken;

/**
 * @version $Id: QueuedAuditorSessionLocal.java 17625 2013-09-20 07:12:06Z netmackan $
 */
@Local
public interface QueuedAuditorSessionLocal extends Auditable {

    /**
     * Prepares the secure audit log mechanism for reset.
     * This method will block till all audit log processes are completed. 
     * Should be used with caution because once called audit log will not be operational. 
     * Any attempt to log will result in an exception.
     */
    void prepareReset() throws AuditLogResetException;

    /**
     * Resets all security audit events logger internal state.
     * Once this method finishes the audit log will be available again.
     * This method should be used with caution.
     */
    void reset() throws AuditLogResetException;
    
    
    void delete(AuthenticationToken token, Date timestamp);

}
