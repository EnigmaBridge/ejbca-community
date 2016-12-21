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

package org.ejbca.ui.cli.config.cmp;

import org.ejbca.config.CmpConfiguration;
import org.ejbca.ui.cli.config.ConfigBaseCommand;

/**
 * Shows the current server configuration
 * 
 * @version $Id: BaseCmpConfigCommand.java 19968 2014-10-09 13:13:58Z mikekushner $
 */
public abstract class BaseCmpConfigCommand extends ConfigBaseCommand {
     
    private CmpConfiguration cmpConfiguration = null;

    @Override
    public String[] getCommandPath() {
        return new String[] { super.getCommandPath()[0] , "cmp" };
    }
    
    protected CmpConfiguration getCmpConfiguration() {
        if (cmpConfiguration == null) {
            cmpConfiguration = (CmpConfiguration) getGlobalConfigurationSession().getCachedConfiguration(CmpConfiguration.CMP_CONFIGURATION_ID);
        }
        return cmpConfiguration;
    }

}
