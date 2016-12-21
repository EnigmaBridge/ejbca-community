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
package org.cesecore.authentication.tokens;

import java.io.Serializable;
import java.security.Principal;

/**
 * The representation of a user name.  
 * 
 * @version $Id: UsernamePrincipal.java 18305 2013-12-16 13:59:56Z anatom $
 */
public class UsernamePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

	final String username;
	
	public UsernamePrincipal(final String username) {
		this.username = username;
	}
	
	@Override
	public String getName() {
		return username;
	}

	@Override
	public String toString() {
		return username;
	}
}
