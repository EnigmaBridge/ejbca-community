
package org.w3._2002._03.xkms_;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PrototypeKeyBindingType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PrototypeKeyBindingType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2002/03/xkms#}KeyBindingAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2002/03/xkms#}ValidityInterval" minOccurs="0"/>
 *         &lt;element ref="{http://www.w3.org/2002/03/xkms#}RevocationCodeIdentifier" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PrototypeKeyBindingType", propOrder = {
    "validityInterval",
    "revocationCodeIdentifier"
})
public class PrototypeKeyBindingType
    extends KeyBindingAbstractType
{

    @XmlElement(name = "ValidityInterval")
    protected ValidityIntervalType validityInterval;
    @XmlElement(name = "RevocationCodeIdentifier")
    protected byte[] revocationCodeIdentifier;

    /**
     * Gets the value of the validityInterval property.
     * 
     * @return
     *     possible object is
     *     {@link ValidityIntervalType }
     *     
     */
    public ValidityIntervalType getValidityInterval() {
        return validityInterval;
    }

    /**
     * Sets the value of the validityInterval property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValidityIntervalType }
     *     
     */
    public void setValidityInterval(ValidityIntervalType value) {
        this.validityInterval = value;
    }

    /**
     * Gets the value of the revocationCodeIdentifier property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getRevocationCodeIdentifier() {
        return revocationCodeIdentifier;
    }

    /**
     * Sets the value of the revocationCodeIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setRevocationCodeIdentifier(byte[] value) {
        this.revocationCodeIdentifier = ((byte[]) value);
    }

}
