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

package org.ejbca.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @version $Id: HTMLToolsTest.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class HTMLToolsTest {


    @Test
    public void test01JavascriptEscape() throws Exception {
        String test = "l'AC si vous l'avez";
        assertEquals("l\\'AC si vous l\\'avez", HTMLTools.javascriptEscape(test));
    }
}
