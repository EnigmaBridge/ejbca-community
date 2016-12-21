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
package org.ejbca.core.ejb.authentication.web;

import javax.ejb.Remote;

import org.cesecore.authentication.tokens.AuthenticationProvider;

/**
 * @version $Id: WebAuthenticationProviderProxySessionRemote.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
@Remote
public interface WebAuthenticationProviderProxySessionRemote  extends AuthenticationProvider {

}
