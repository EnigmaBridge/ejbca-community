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
package org.cesecore.certificates.ca;

import org.cesecore.CaTestUtils;
import org.cesecore.RoleUsingTestCase;
import org.cesecore.SystemTestsConfiguration;
import org.cesecore.keys.token.CryptoTokenTestUtils;
import org.cesecore.util.CryptoProviderTools;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the CA session bean using PKCS11 HSM crypto token for the CA.
 * When using PKCS11 tokens some tests run with soft tokens are not relevant, since you can never copy the 
 * private key across a remote link, and the PKCS#11 sesison when updated (key gen) in one JVM is not
 * updated in another JVM with JVM restart between.
 * 
 * @version $Id: CaPKCS11SessionTest.java 19366 2014-07-15 15:45:13Z anatom $
 */
public class CaPKCS11SessionTest extends RoleUsingTestCase {

    private static final String X509CADN = "CN=TESTP11";
    private static CA authenticationx509ca = null;
    private static final char[] tokenpin = SystemTestsConfiguration.getPkcs11SlotPin("userpin1");
    
    private static CaSessionTestBase testBase;

    @BeforeClass
    public static void setUpProviderAndCreateCA() throws Exception {
        CryptoProviderTools.installBCProvider();
        // Initialize role system
        setUpAuthTokenAndRole("CaPKCS11SessionTestRoleInitialization");
        // Don't use PKCS11 for the authentication CA, it's not part of the P11 test
        authenticationx509ca = CaTestUtils.createTestX509CA(X509CADN, tokenpin, false);
        testBase = new CaSessionTestBase(authenticationx509ca, null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            CryptoTokenTestUtils.removeCryptoToken(null, authenticationx509ca.getCAToken().getCryptoTokenId());
        } finally {
            // Be sure to to this, even if the above fails
            tearDownRemoveRole();
        }
    }

    @Before
    public void setUp() throws Exception {
        testBase.setUp();
    }

    @After
    public void tearDown() throws Exception {
        testBase.tearDown();
    }

    @Test
    public void addCAGenerateKeysLater() throws Exception {
        final String cadn = "CN=TEST GEN KEYS, O=CaPKCS11SessionTest, C=SE";
        final CA ca = CaTestUtils.createTestX509CAOptionalGenKeys(cadn, tokenpin, false, true);
        testBase.addCAGenerateKeysLater(ca, cadn, tokenpin);
    }
    
    @Test
    public void addCAUseSessionBeanToGenerateKeys2() throws Exception {
        final String cadn = "CN=TEST GEN KEYS, O=CaPKCS11SessionTest, C=SE";
        final CA ca = CaTestUtils.createTestX509CAOptionalGenKeys(cadn, tokenpin, false, true);
        testBase.addCAUseSessionBeanToGenerateKeys(ca, cadn, tokenpin);
    }

}
