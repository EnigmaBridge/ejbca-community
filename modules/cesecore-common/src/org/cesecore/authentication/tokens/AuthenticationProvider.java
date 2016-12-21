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

/**
 * This is a callback interface that provides a method of authentication for a subject. It should be implemented by whatever EJB Session bean (outside
 * of CESeCore) that perform local authentication.
 * 
 * @version $Id: AuthenticationProvider.java 18305 2013-12-16 13:59:56Z anatom $
 * 
 */
public interface AuthenticationProvider extends Serializable {

    static final String DEFAULT_DN = "C=SE,O=Test,CN=Test"; // default
    
    /**
     * Implement this method to authenticate a subject using its principals and credentials. The method of doing this operation is entirely up to
     * whoever implements this API. The returned AuthenticationToken should only contain those principals and credentials which were actually used in
     * the authentication process.
     * 
     * @param subject an AuthenticationSubject containing a set if principals and/or a set of credentials, the contents required depends in the implementation of this method.
     * @return an AuthenticationToken if the subject was authenticated, null otherwise.
     */
    AuthenticationToken authenticate(AuthenticationSubject subject);

}
