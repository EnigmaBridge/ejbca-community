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
package org.ejbca.ui.cli.infrastructure.parameter.enums;

/**
 * Represents if a Parameter is mandatory or not. 
 * 
 * @version $Id: MandatoryMode.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public enum MandatoryMode {
    MANDATORY(true), OPTIONAL(false);
    
    private final boolean isMandatory;
    
    private MandatoryMode(boolean isMandatory) {
        this.isMandatory = isMandatory;
    }
    
    public boolean isMandatory() {
        return isMandatory;
    }
}
