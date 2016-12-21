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
package org.ejbca.core.protocol.cmp;

import org.cesecore.certificates.certificate.request.RequestMessage;


/**
 * The {link IRequestMessage} parameter must implement this to when calling {@link CrmfRequestMessage#createResponseMessage(Class, IRequestMessage, java.security.cert.Certificate, java.security.PrivateKey, String)}
 * @author primelars
 * @version $Id: ICrmfRequestMessage.java 19901 2014-09-30 14:29:38Z anatom $
 */
public interface ICrmfRequestMessage extends RequestMessage {

	int getPbeIterationCount();

	String getPbeDigestAlg();

	String getPbeMacAlg();

	String getPbeKeyId();

	String getPbeKey();

}
