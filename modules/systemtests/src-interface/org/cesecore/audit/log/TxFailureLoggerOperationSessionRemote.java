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
 
import javax.ejb.Remote;

/**
 * 
 * @version $Id: TxFailureLoggerOperationSessionRemote.java 12187 2011-07-27 09:35:18Z mikekushner $
 *
 */

@Remote
public interface TxFailureLoggerOperationSessionRemote {

    void willLaunchExceptionAfterLog() throws Exception;

}

