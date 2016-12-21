
package org.ejbca.core.protocol.ws.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "editUser", namespace = "http://ws.protocol.core.ejbca.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "editUser", namespace = "http://ws.protocol.core.ejbca.org/")
public class EditUser {

    @XmlElement(name = "arg0", namespace = "")
    private org.ejbca.core.protocol.ws.objects.UserDataVOWS arg0;

    /**
     * 
     * @return
     *     returns UserDataVOWS
     */
    public org.ejbca.core.protocol.ws.objects.UserDataVOWS getArg0() {
        return this.arg0;
    }

    /**
     * 
     * @param arg0
     *     the value for the arg0 property
     */
    public void setArg0(org.ejbca.core.protocol.ws.objects.UserDataVOWS arg0) {
        this.arg0 = arg0;
    }

}
