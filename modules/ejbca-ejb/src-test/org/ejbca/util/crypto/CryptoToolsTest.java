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
package org.ejbca.util.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * This class contains unit tests for the CryptoTools class.
 * 
 * @version $Id: CryptoToolsTest.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class CryptoToolsTest {
    
    @Test
    public void testExtractSaltFromPasswordHash() {
        //Firstly, generate a hash.
        final String password = "greenunicornisfine";
        final String salt = BCrypt.gensalt(1);
        final String passwordHash = BCrypt.hashpw(password, salt);
        
        String extractedSalt = CryptoTools.extractSaltFromPasswordHash(passwordHash);
        assertEquals(salt, extractedSalt);
    }

}
