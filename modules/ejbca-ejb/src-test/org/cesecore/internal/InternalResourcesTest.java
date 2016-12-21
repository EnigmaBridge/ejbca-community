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
package org.cesecore.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @version $Id: InternalResourcesTest.java 19692 2014-09-03 22:11:46Z davidcarella $
 */
public class InternalResourcesTest {

    /** Note diff between EJBCA and CESeCore */
    private static final String TEST_RESOURCE_LOCATION = "/intresources";
    // Classpath issues, use "src/intresources" when running from within eclipse
    //private static final String TEST_RESOURCE_LOCATION = "src/intresources";
	
    @Test
    public void testGetLocalizedMessageString() {
        InternalResources intres = new InternalResources(TEST_RESOURCE_LOCATION);
        String res = intres.getLocalizedMessage("raadmin.testmsg");
        assertEquals("Test en-US                        ", res);
        assertEquals("Test en-US                        ", intres.getLocalizedMessageCs("raadmin.testmsg").toString());
        // This message will only exist in the secondary language file
        res = intres.getLocalizedMessage("raadmin.testmsgsv");
        assertEquals("Test sv-SE", res);
        assertEquals("Test sv-SE", intres.getLocalizedMessageCs("raadmin.testmsgsv").toString());
    }

    @Test
    public void testGetUnfoundMessageButPreserveParameters() {
        InternalResources intres = new InternalResources();
        String result = intres.getLocalizedMessage("foo", "a", "b");
        assertEquals("foo, a, b", result);
    }
    
    @Test
    public void testNonExistingLocalizedMessageString() {
        InternalResources intres = new InternalResources(TEST_RESOURCE_LOCATION);
        String res = intres.getLocalizedMessage("raadmin.foo");
        assertEquals("raadmin.foo", res);
        assertEquals("raadmin.foo", intres.getLocalizedMessageCs("raadmin.foo").toString());
    }

    @Test
    public void testGetLocalizedMessageStringObject() {
        InternalResources intres = new InternalResources(TEST_RESOURCE_LOCATION);
        String res = intres.getLocalizedMessage("raadmin.testparams", Long.valueOf(1), Integer.valueOf(3), "hi", Boolean.TRUE, "bye");
        assertEquals("Test 1 3 hi true bye message 1 ", res);
        assertEquals("Test 1 3 hi true bye message 1 ", intres.getLocalizedMessageCs("raadmin.testparams", Long.valueOf(1), Integer.valueOf(3), "hi", Boolean.TRUE, "bye").toString());
    }

    @Test
    public void testGetLocalizedMessageStringObjectWithNull() {
        InternalResources intres = new InternalResources(TEST_RESOURCE_LOCATION);
        String res = intres.getLocalizedMessage("raadmin.testparams", null, Integer.valueOf(3), null, Boolean.TRUE, "bye");
        assertEquals("Test  3  true bye message  ", res);
        assertEquals("Test  3  true bye message  ", intres.getLocalizedMessageCs("raadmin.testparams", null, Integer.valueOf(3), null, Boolean.TRUE, "bye").toString());

        res = intres.getLocalizedMessage("raadmin.testparams");
        assertEquals("Test      message  ", res);
        assertEquals("Test      message  ", intres.getLocalizedMessageCs("raadmin.testparams").toString());
    }

    @Test
    public void testMessageStringWithExtraParameter() {
        InternalResources intres = new InternalResources(TEST_RESOURCE_LOCATION);
        String res = intres.getLocalizedMessage("raadmin.testmsgsv");
        assertEquals("Test sv-SE", res);
        assertEquals("Test sv-SE", intres.getLocalizedMessageCs("raadmin.testmsgsv").toString());
        res = intres.getLocalizedMessage("raadmin.testmsgsv", "foo $bar \\haaaar");
        assertEquals("Test sv-SE", res);
        assertEquals("Test sv-SE", intres.getLocalizedMessageCs("raadmin.testmsgsv", "foo $bar \\haaaar").toString());
    }
}
