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
package org.cesecore.certificates.ocsp.logging;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @version $Id: TransactionCounterTest.java 14320 2012-03-13 08:07:14Z mikekushner $
 *
 */
public class TransactionCounterTest {

    @Test
    public void testTransactionCounter() {
        assertEquals(0, TransactionCounter.INSTANCE.getTransactionNumber());
        assertEquals(1, TransactionCounter.INSTANCE.getTransactionNumber());
    }
}
