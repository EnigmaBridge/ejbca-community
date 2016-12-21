package org.ejbca.core.protocol.ws.client.gen;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getCertificatesByExpirationTimeAndTypeResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getCertificatesByExpirationTimeAndTypeResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="return" type="{http://ws.protocol.core.ejbca.org/}certificate" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getCertificatesByExpirationTimeAndTypeResponse", propOrder = {
    "_return"
})
public class GetCertificatesByExpirationTimeAndTypeResponse {

    @XmlElement(name = "return")
    protected List<Certificate> _return;

    /**
     * Gets the value of the return property.
     * 
     * Objects of the following type(s) are allowed in the list
     * {@link Certificate }
     * 
     * 
     */
    public List<Certificate> getReturn() {
        if (_return == null) {
            _return = new ArrayList<Certificate>();
        }
        return this._return;
    }

}