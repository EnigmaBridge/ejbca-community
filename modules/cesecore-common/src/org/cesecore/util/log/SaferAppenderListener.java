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

package org.cesecore.util.log;

/**
 * Interface implemented by the client of the SaferDailyRollingFileAppender.
 * 
 * Since the appender is used from JBoss we can't just throw an Exception and
 * need this to communicate errors.
 * 
 * @version  $Id: SaferAppenderListener.java 17647 2013-09-20 14:02:02Z netmackan $
 */
public interface SaferAppenderListener {

	public abstract void setCanlog(boolean pCanlog);

}
