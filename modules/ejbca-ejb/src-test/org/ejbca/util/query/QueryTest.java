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

package org.ejbca.util.query;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.junit.Test;


/**
 * Tests the CertTools class .
 *
 * @version $Id: QueryTest.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class QueryTest {
    private static Logger log = Logger.getLogger(QueryTest.class);

    @Test
    public void test01TestUserQuery() throws Exception {
        log.trace(">test01TestUserQuery()");
        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_STATUS, BasicMatch.MATCH_TYPE_EQUALS, Integer.toString(EndEntityConstants.STATUS_NEW));
        String str = query.getQueryString();
        assertEquals("status = 10", str);

        query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_USERNAME, BasicMatch.MATCH_TYPE_EQUALS, "foo");
        str = query.getQueryString();
        assertEquals("username = 'foo'", str);
        
        log.trace("<test01TestUserQuery()");
    }

}
