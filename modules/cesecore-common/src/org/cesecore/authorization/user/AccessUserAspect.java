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

package org.cesecore.authorization.user;

import java.io.Serializable;

/**
 * Interface for AccessUserAspectData.  
 * 
 * @version $Id: AccessUserAspect.java 17625 2013-09-20 07:12:06Z netmackan $
 *
 */
public interface AccessUserAspect extends Serializable {

    int getMatchWith();

    void setMatchWith(Integer matchWith);

    int getMatchType();

    void setMatchType(Integer matchType);

    void setMatchTypeAsValue(AccessMatchType matchType);

    AccessMatchType getMatchTypeAsType();

    String getMatchValue();

    void setMatchValue(String matchValue);

    Integer getCaId();

    void setCaId(Integer caId);

    String getTokenType();

    void setTokenType(String tokenType);
   
}