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

package org.ejbca.util;

import java.util.HashMap;
import java.util.Map;

import org.cesecore.util.StringTools;


/**  Only used for backwards compatibility with earlier versions of EJBCA
 * @see org.cesecore.util.Base64PutHashMap
 * 
 * @version $Id: Base64GetHashMap.java 19901 2014-09-30 14:29:38Z anatom $
 */
public class Base64GetHashMap extends HashMap<Object, Object> {
    private static final long serialVersionUID = -6270344460163780577L;

    public Base64GetHashMap() {
        super();
    }

    public Base64GetHashMap(Map<Object, Object> m) {
        super(m);
    }
    
    public Object get(Object key) {
        Object o = super.get(key);
        if (o == null) {
            return o;
        }
        if (o instanceof String) {
            String s = (String) o;
            return StringTools.getBase64String(s);                       
        }
        return o;
    }
    
}
