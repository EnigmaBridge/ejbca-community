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
package org.ejbca.core.protocol.certificatestore;




/** class used from TestCertificateCache, depends on TestCertificateCache
 * @author tomas
 * @version $Id: CacheExceptionHandler.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class CacheExceptionHandler implements Thread.UncaughtExceptionHandler {
	public void uncaughtException(Thread t, Throwable e) { // NOPMD, this is not a JEE app, only a test
		CaCertificateCacheTest.threadException = e;
	}
}
