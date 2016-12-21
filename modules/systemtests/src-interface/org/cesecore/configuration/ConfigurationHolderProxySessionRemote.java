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
package org.cesecore.configuration;

import javax.ejb.Remote;

/**
 * Acts as a proxy for ConfigurationHolder tests
 * 
 * @version $Id: ConfigurationHolderProxySessionRemote.java 12183 2011-07-27 07:18:03Z mikekushner $
 *
 */
@Remote
public interface ConfigurationHolderProxySessionRemote {

    public String getDefaultValue(String key);
    
}
