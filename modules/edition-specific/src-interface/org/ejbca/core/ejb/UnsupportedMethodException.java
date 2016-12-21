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

package org.ejbca.core.ejb;

import org.cesecore.CesecoreException;
import org.cesecore.ErrorCode;

/**
 * Thrown when accessing enterprise-edition-only features in the community version
 * @version $Id: UnsupportedMethodException.java 20988 2015-03-24 18:20:08Z aveen4711 $
 *
 */
public class UnsupportedMethodException extends CesecoreException {
    

    private static final long serialVersionUID = -6099472895840497282L;

    /**
     * @param message with more information what is wrong
     */
    public UnsupportedMethodException(String m) {
        super(ErrorCode.UNSUPPORTED_METHOD, m);
    }
}
