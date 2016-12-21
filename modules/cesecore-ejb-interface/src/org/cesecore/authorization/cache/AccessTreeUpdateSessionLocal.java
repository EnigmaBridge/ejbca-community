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
package org.cesecore.authorization.cache;

import javax.ejb.Local;

/**
 * @version $Id: AccessTreeUpdateSessionLocal.java 20608 2015-02-02 11:43:15Z jeklund $
 */
@Local
public interface AccessTreeUpdateSessionLocal {

    /**
     * Method incrementing the authorization tree update number and thereby
     * signaling to other beans that they should reconstruct their access trees.
     */
    void signalForAccessTreeUpdate();
    
    /**
     * Method returning the newest authorizationtreeupdatenumber.
     * Should be checked when the access tree cache has expired to avoid rebuilding the tree if there are no database changes. 
     */
    int getAccessTreeUpdateNumber();

}
