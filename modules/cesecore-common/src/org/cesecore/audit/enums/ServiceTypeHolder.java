package org.cesecore.audit.enums;

/**
 * Simple implementation of ServiceType that holds the identifier.
 * 
 * @version $Id: ServiceTypeHolder.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public class ServiceTypeHolder implements ServiceType {

    private static final long serialVersionUID = 1L;

    private final String value;
    
    public ServiceTypeHolder(final String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    @Override
    public boolean equals(final ServiceType value) {
        if (value == null) {
            return false;
        }
        return this.value.toString().equals(value);
    }
}
