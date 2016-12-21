/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb;

import javax.ejb.Local;

/**
 * JEE5 Lookup helper implementation for optional (enterprise edition) EJBs.
 * 
 * @version $Id: EnterpriseEditionEjbBridgeSessionLocal.java 20986 2015-03-24 16:23:17Z mikekushner $
 */
@Local
public interface EnterpriseEditionEjbBridgeSessionLocal {

    <T> T getEnterpriseEditionEjbLocal(Class<T> localInterfaceClass, String modulename);
    
    /**
     * A simple function allowing the implementation to answer whether elements only available in Enterprise are present, in cases 
     * where the elements in question might not be available on the classpath across the application. 
     * 
     * @return true if running EJBCA Enterprise Edition
     */
    boolean isRunningEnterprise();
}
