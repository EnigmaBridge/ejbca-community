
package org.ejbca.core.protocol.ws.jaxws;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "genTokenCertificates", namespace = "http://ws.protocol.core.ejbca.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "genTokenCertificates", namespace = "http://ws.protocol.core.ejbca.org/", propOrder = {
    "arg0",
    "arg1",
    "arg2",
    "arg3",
    "arg4"
})
public class GenTokenCertificates {

    @XmlElement(name = "arg0", namespace = "")
    private org.ejbca.core.protocol.ws.objects.UserDataVOWS arg0;
    @XmlElement(name = "arg1", namespace = "")
    private List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> arg1;
    @XmlElement(name = "arg2", namespace = "")
    private org.ejbca.core.protocol.ws.objects.HardTokenDataWS arg2;
    @XmlElement(name = "arg3", namespace = "")
    private boolean arg3;
    @XmlElement(name = "arg4", namespace = "")
    private boolean arg4;

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

    /**
     * 
     * @return
     *     returns List<TokenCertificateRequestWS>
     */
    public List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> getArg1() {
        return this.arg1;
    }

    /**
     * 
     * @param arg1
     *     the value for the arg1 property
     */
    public void setArg1(List<org.ejbca.core.protocol.ws.objects.TokenCertificateRequestWS> arg1) {
        this.arg1 = arg1;
    }

    /**
     * 
     * @return
     *     returns HardTokenDataWS
     */
    public org.ejbca.core.protocol.ws.objects.HardTokenDataWS getArg2() {
        return this.arg2;
    }

    /**
     * 
     * @param arg2
     *     the value for the arg2 property
     */
    public void setArg2(org.ejbca.core.protocol.ws.objects.HardTokenDataWS arg2) {
        this.arg2 = arg2;
    }

    /**
     * 
     * @return
     *     returns boolean
     */
    public boolean isArg3() {
        return this.arg3;
    }

    /**
     * 
     * @param arg3
     *     the value for the arg3 property
     */
    public void setArg3(boolean arg3) {
        this.arg3 = arg3;
    }

    /**
     * 
     * @return
     *     returns boolean
     */
    public boolean isArg4() {
        return this.arg4;
    }

    /**
     * 
     * @param arg4
     *     the value for the arg4 property
     */
    public void setArg4(boolean arg4) {
        this.arg4 = arg4;
    }

}
