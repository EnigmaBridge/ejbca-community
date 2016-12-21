package org.cesecore.audit.enums;

import java.io.Serializable;

/**
 *  Generic constant type holder.
 * 
 * @version $Id: ConstantType.java 17625 2013-09-20 07:12:06Z netmackan $
 * 
 */
public interface ConstantType<T extends ConstantType<T>> extends Serializable {
    boolean equals(final T value);
    String toString();
}
