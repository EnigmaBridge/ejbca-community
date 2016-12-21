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
package org.ejbca.ui.web.admin.services.servicetypes;


/**
 * Class managing the view of the Certificate Expiration Notifier Worker
 *
 * @version $Id: CertificateExpirationNotifierWorkerType.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class CertificateExpirationNotifierWorkerType extends BaseEmailNotifyingWorkerType {

	private static final long serialVersionUID = -3680823921964522760L;
    public static final String NAME = "CERTNOTIFICATIONWORKER";
		
	public CertificateExpirationNotifierWorkerType(){
		super(NAME, "certnotificationworker.jsp", org.ejbca.core.model.services.workers.CertificateExpirationNotifierWorker.class.getName());
		
	}
}
