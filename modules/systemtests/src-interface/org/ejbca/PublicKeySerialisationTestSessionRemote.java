/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca;

import java.security.PublicKey;

import javax.ejb.Remote;

/**
 * @version $Id: PublicKeySerialisationTestSessionRemote.java 20792 2015-03-02 10:56:53Z mikekushner $
 *
 */
@Remote
public interface PublicKeySerialisationTestSessionRemote {

    PublicKey getKey();
}
