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
package org.cesecore.certificates.ca.extendedservices;

import java.io.Serializable;

/**
 * Should be inherited by all ExtendedCAServiceRequest Value objects.  
 *
 * @version $Id: ExtendedCAServiceRequest.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public abstract class ExtendedCAServiceRequest  implements Serializable {    

    private static final long serialVersionUID = 1361862486247861779L;

    public ExtendedCAServiceRequest(){}    
    
    /** Must return a value that can be used by the CA to retrieve the service. I.e. the type must be the same 
     * that was used in ExtendedCAServiceInfo when creating the CA service.
     * 
     * @return type flag, for example ExtendedCAServiceInfo.TYPE_CMSEXTENDEDSERVICE
     */
    public abstract int getServiceType();
    
}
