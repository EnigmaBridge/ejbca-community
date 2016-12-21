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
package org.cesecore.junit.util;

import java.util.ArrayList;
import java.util.List;

import org.cesecore.keys.token.CryptoTokenTestUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.Suite;
import org.junit.runners.model.Statement;

/**
 * This test runner will make sure that any system test marked to use it will run twice: once with a soft crypto token and once with a PKCS11-token if available. 
 * 
 * @version $Id: CryptoTokenTestRunner.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class CryptoTokenTestRunner extends Suite {
    
    public CryptoTokenTestRunner(Class<?> klass) throws Exception {
        super(klass, getRunners(klass));
    }

    /**
     * 
     * @return a list of test runners. By default will only return a runner that returns soft tokens, but will include a runner for PKCS#11 tokens as well, if available. 
     * @throws Exception 
     */
    private static List<Runner> getRunners(Class<?> klass) throws Exception {
        List<Runner> runners = new ArrayList<Runner>();
        if (CryptoTokenTestUtils.getHSMLibrary() != null) {
            runners.add(new PKCS11TestRunner(klass));
        }
        runners.add(new PKCS12TestRunner(klass));
        return runners;
    }

    @Override
    public void run(final RunNotifier notifier) {
        EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
        try {
            Statement statement = classBlock(notifier);
            statement.evaluate();
        } catch (AssumptionViolatedException e) {
            testNotifier.fireTestIgnored();
        } catch (StoppedByUserException e) {
            throw e;
        } catch (Throwable e) {
            testNotifier.addFailure(e);
        }
    }

    /*
     * Special version of the classBlock method that avoids @BeforeClass and @AfterClass annotations and class rules on tests that are treated as suites.
     */
    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement = childrenInvoker(notifier);
        return statement;
    }
    
    

}
