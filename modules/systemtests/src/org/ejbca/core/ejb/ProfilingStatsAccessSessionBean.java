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

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.cesecore.jndi.JndiConstants;

/**
 * Proxy to make ProfilingStats accessible over RMI.
 * 
 * @version $Id: ProfilingStatsAccessSessionBean.java 19902 2014-09-30 14:32:24Z anatom $
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "ProfilingStatsAccessSessionRemote")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProfilingStatsAccessSessionBean implements ProfilingStatsAccessSessionRemote {

    @Override
    public List<ProfilingStat> getEjbInvocationStats() {
        return ProfilingStats.INSTANCE.getEjbInvocationStats();
    }

}
