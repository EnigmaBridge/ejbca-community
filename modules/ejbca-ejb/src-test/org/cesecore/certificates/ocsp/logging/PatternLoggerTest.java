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

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Test the PatternLogger  
 * 
 * @version $Id: PatternLoggerTest.java 17428 2013-08-14 10:50:16Z anatom $
 */
public class PatternLoggerTest {

    private static final Logger log = Logger.getLogger(PatternLoggerTest.class);

    /** Try out some interpolation with focus on different date formats. */
    @Test
    public void testPatternLoggerDateFormats() throws Exception {
        log.trace(">testPatternLogger");
        final String LOG_PATTERN = "${VAR1};\"${VAR2}\";${" + PatternLogger.LOG_TIME + "};${" + PatternLogger.LOG_ID + "};${VAR3}";
        testPatternLoggerInternal(LOG_PATTERN, "yyyy-MM-dd:HH:mm:ss:z", "GMT",
                "^content1;\"content2\";\\d{4}-\\d{2}-\\d{2}:\\d{2}:\\d{2}:\\d{2}:GMT;0;content3$");
        testPatternLoggerInternal(LOG_PATTERN, "yyyy-MM-dd HH:mm:ssZ", "CET",
                "^content1;\"content2\";\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\+0\\d00;0;content3$");
        testPatternLoggerInternal(LOG_PATTERN, "yyyy-MM-dd'T'HH:mm:ssZ", "CET",
                "^content1;\"content2\";\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+0\\d00;0;content3$");
        log.trace("<testPatternLogger");
    }

    /** Helper method that replaces all ${VARx} where x={0..10} with "contentx" and asserts that the result is the expected using regexp. */
    private void testPatternLoggerInternal(String pattern, String dateFormat, String timeZone, String expected) throws Exception {
        log.trace(">testPatternLoggerInternal");
        final PatternLogger patternLogger = new TestPatternLogger("\\$\\{(.+?)\\}", pattern, dateFormat, timeZone);
        
        for (int i = 0; i < 10; i++) {
            patternLogger.paramPut("VAR" + i, "content" + i);
        }
        // We reference the private method here, which is a bit ugly but works.
        final Method m = PatternLogger.class.getDeclaredMethod("interpolate", new Class[0]);
        m.setAccessible(true);
        final String result = (String) m.invoke(patternLogger);
        log.debug("result: " + result);
        assertTrue("Result of interpolation operation did not match expected result.", result.matches(expected));
        log.trace("<testPatternLoggerInternal");
    }
    
    private class TestPatternLogger extends PatternLogger {

        private static final long serialVersionUID = -8096112483409703781L;

        protected TestPatternLogger(String matchPattern, String matchString, String logDateFormat, String timeZone) {
            super(false, PatternLoggerTest.class, matchPattern, matchString, logDateFormat, timeZone);
            
        }
        
    }
}
