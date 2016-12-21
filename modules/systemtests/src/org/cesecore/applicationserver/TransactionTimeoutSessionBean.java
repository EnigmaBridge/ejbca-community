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
package org.cesecore.applicationserver;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.cesecore.jndi.JndiConstants;

/**
 * @version $Id: TransactionTimeoutSessionBean.java 12583 2011-09-15 08:46:19Z anatom $
 *
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "TransactionTimeoutSessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TransactionTimeoutSessionBean implements TransactionTimeoutSessionRemote {

    @Override
    public int timeout(long sleepTime) throws InterruptedException {
        Thread.sleep(sleepTime);
        return 1337;
    }

}
