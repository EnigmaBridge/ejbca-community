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
package org.cesecore.mock.authentication;

import javax.ejb.Remote;

import org.cesecore.authentication.tokens.AuthenticationProvider;

/**
 * This remote interface represents a trivial implementation of an AuthenticationProvider. This is merely a test resource and shouldn't be confused for real
 * code.
 * 
 * 
 * @version $Id: SimpleAuthenticationProviderSessionRemote.java 20746 2015-02-25 14:52:01Z mikekushner $
 */
@Remote
public interface SimpleAuthenticationProviderSessionRemote extends AuthenticationProvider {
}
