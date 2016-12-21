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

package org.cesecore.authorization.user.matchvalues;

/**
 * Interface for all AccessMatchValue implementations.
 * 
 * @version $Id: AccessMatchValue.java 18613 2014-03-17 13:31:40Z mikekushner $
 *
 */
public interface AccessMatchValue {

    /**
     * 
     * @return the numeric value of this AccessMatchValue, i.e. its database value. 
     */
    int getNumericValue();
    

    /** @return true if this is a preferred value */
    boolean isDefaultValue();

    /**
     * A string value inherent to the implementing AccessMatchValue. This value should be unique, but independent of code 
     * (i.e do not use Class.getSimpleName()) to avoid upgrade issues in case of future refactorization.
     * 
     * @return a name for the implementation of this match value. 
     */
    String getTokenType();
    
    /**
     * 
     * @return the name of the implementing enumeration.
     */
    String name();
    
    /**
     * 
     * @return true of this AccessMatchValue is issued by a CA 
     */
    boolean isIssuedByCa();
}