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
package org.ejbca.ui.cli;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Basic system test for the ClearCacheCommand
 * 
 * @version $Id: ClearCacheCommandTest.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class ClearCacheCommandTest {

    private ClearCacheCommand command = new ClearCacheCommand();

    @Test
    public void testWithAllOptions() {
        String[] args = new String[] { "-all", "-globalconf", "-eeprofile", "-certprofile", "-authorization", "-ca" };
        try {
            command.execute(args);
        } catch (Exception e) {
            //Fail on any exception
            fail("Command did not execute correctly");
        }
    }
}
