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
package org.ejbca.core.model.services.workers;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.services.BaseWorker;
import org.ejbca.core.model.services.ServiceExecutionFailedException;

/**
 * Worker renewing CA that is about to expire.
 * 
 *
 * @version: $Id: RenewCAWorker.java 19901 2014-09-30 14:29:38Z anatom $
 */
public class RenewCAWorker extends BaseWorker {

	private static final Logger log = Logger.getLogger(RenewCAWorker.class);
    /** Internal localization of logs and errors */
	private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();	

	/** Flag is keys should be regenerated or not */
	public static final String PROP_RENEWKEYS           = "worker.renewkeys";
	
	/**
	 * Worker that makes a query to the Certificate Store about
	 * expiring certificates.
	 * 
	 * @see org.ejbca.core.model.services.IWorker#work()
	 */
	public void work(Map<Class<?>, Object> ejbs) throws ServiceExecutionFailedException {
		log.trace(">Worker started");
        final CAAdminSessionLocal caAdminSession = ((CAAdminSessionLocal)ejbs.get(CAAdminSessionLocal.class));
        final CaSessionLocal caSession = ((CaSessionLocal)ejbs.get(CaSessionLocal.class));
		
		// Find CAs with expire date that is less than configured number of days 
		// ahead in the future		
		long millistoexpire = getTimeBeforeExpire();
		long now = new Date().getTime();
		long expiretime = now + millistoexpire;

		// Renew these CAs using the CAAdminSessionBean		
		// Check the "Generate new keys" checkbox so we can pass the correct parameter to CAAdminSessionBean
		Collection<Integer> caids = getCAIdsToCheck(false);
		log.debug("Checking renewal for "+caids.size()+" CAs");
		Iterator<Integer> iter = caids.iterator();
		while (iter.hasNext()) {
			Integer caid = iter.next();
			try {
				CAInfo info = caSession.getCAInfo(getAdmin(), caid.intValue());
				String caname = null;
				if (info != null) {
					caname = info.getName();
					Date expire = info.getExpireTime(); 
					log.debug("CA "+caname+" expires on "+expire);
					if (expire.before(new Date(expiretime))) {
						// Only try to renew active CAs
						// There should be other monitoring available to check if CAs that should not be off-line are off-line (HealthCheck)
					    try {
					        final boolean createLinkCertificate = isRenewKeys();   // We want link certs for new key..
					        caAdminSession.renewCA(getAdmin(), info.getCAId(), isRenewKeys(), null, createLinkCertificate);
					    } catch (CryptoTokenOfflineException e) {
					        log.info("Not trying to renew CA because CA and token status are not on-line.");
					    }
					}
				} else {
					String msg = intres.getLocalizedMessage("services.errorworker.errornoca", caid, caname);
					log.error(msg);
				}
			} catch (CADoesntExistsException e) {
				log.error("Error renewing CA: ", e);
			} catch (AuthorizationDeniedException e) {
				log.error("Error renewing CA: ", e);
			}				
		}
		log.trace("<Worker ended");
	}
	
	protected boolean isRenewKeys() {
		return properties.getProperty(PROP_RENEWKEYS,"FALSE").equalsIgnoreCase("TRUE");
	}
}
