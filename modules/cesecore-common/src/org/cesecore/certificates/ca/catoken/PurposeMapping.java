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
package org.cesecore.certificates.ca.catoken;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;



/** Class wraps keystring properties. The properties passed in to it can contain fields as the constants:
 * 
 * The values of the properties consists of purposeproperty and keyalias
 * <pre>
 *    certSignKey fooalias02
 *    crlSignKey fooalias02
 *    keyEncryptKey fooencalias
 *    hardTokenEncrypt fooencalias
 *    previousCertSignKey fooalias01
 *    nextCertSignKey fooalias03
 *    testKey testalias
 *    defaultKey defaultalias
 * </pre>
 *  When the strings are added they are mapped to different key purposes, CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN etc. 
 *  When the method getString is called with CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN it will return fooalias, if getString is called
 *  with a key purpose that was not specified, for example CryptoTokenConstants.CAKEYPURPOSE_KEYENCRYPT it will return defaultalias.
 *  
 *   The returned values are supposed to be used to get keys for different aliases from a keystore.
 * 
 * @version $Id: PurposeMapping.java 17625 2013-09-20 07:12:06Z netmackan $
 */
public final class PurposeMapping {
    
    final private Map<Integer, String> map;
    final private Map<Integer, String> keymap;
    final private String defaultKeyS;
    
    /** 
     * Key string properties with entries consisting of one of the fixed key strings mapping to a key alias. The key alias is user defined.
	 * <pre>
	 *    certSignKey fooalias02
	 *    crlSignKey fooalias02
	 *    keyEncryptKey fooencalias
	 *    hardTokenEncrypt fooencalias
	 *    previousCertSignKey fooalias01
	 *    nextCertSignKey fooalias03
	 *    testKey testalias
	 *    defaultKey defaultalias
	 * </pre>
     * 
     * @param properties key string properties
     */
    public PurposeMapping(final Properties properties) {
    	/** Map of keypurpose integer (CATokenConstants.CAKEYPURPOSE_CERTSIGN) and alias string as defined in Properties */
    	map = new Hashtable<Integer, String>();
    	/** Map of keypurpose integer (CATokenConstants.CAKEYPURPOSE_CERTSIGN) and key purpose string (CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING)
    	 * for the properties defined in Properties */
    	keymap = new Hashtable<Integer, String>();
    	String tmpS = null;
    	if (properties != null) {
    		tmpS = properties.getProperty(CATokenConstants.CAKEYPURPOSE_DEFAULT_STRING);
    		addKey(CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING,
    				CATokenConstants.CAKEYPURPOSE_CERTSIGN,
    				properties);
    		addKey(CATokenConstants.CAKEYPURPOSE_CRLSIGN_STRING,
    				CATokenConstants.CAKEYPURPOSE_CRLSIGN,
    				properties);
    		addKey(CATokenConstants.CAKEYPURPOSE_KEYENCRYPT_STRING,
    				CATokenConstants.CAKEYPURPOSE_KEYENCRYPT,
    				properties);
    		addKey(CATokenConstants.CAKEYPURPOSE_TESTKEY_STRING,
    				CATokenConstants.CAKEYPURPOSE_KEYTEST,
    				properties);
    		addKey(CATokenConstants.CAKEYPURPOSE_HARDTOKENENCRYPT_STRING,
    				CATokenConstants.CAKEYPURPOSE_HARDTOKENENCRYPT,
    				properties);    		
    		addKey(CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING_PREVIOUS,
    				CATokenConstants.CAKEYPURPOSE_CERTSIGN_PREVIOUS,
    				properties);    		
    		addKey(CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING_NEXT,
    				CATokenConstants.CAKEYPURPOSE_CERTSIGN_NEXT,
    				properties);    		
    	}
    	defaultKeyS = tmpS!=null ? tmpS.trim() : null;
    } 
    private void addKey(final String keyS, final int keyI, final Properties properties) {
        String value = properties.getProperty(keyS);
        if ( value!=null && value.length()>0 ) {
            value = value.trim();
            map.put(Integer.valueOf(keyI), value);
            keymap.put(Integer.valueOf(keyI), keyS);
        }
    }
    /** Returns which key alias string is used for a certain key purpose. 
     * For example for CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN would either a key alias as defined by the property "CAKEYPURPOSE_CERTSIGN_STRING myCertSignKey" (myCertSignKey) 
     * or null be returned. null is returned if no CAKEYPURPOSE_CERTSIGN_STRING (certSignKey) property was specified by the user.
     */ 
    public String getAlias(final int purpose) {
        String s;
        try {
            s = (String)map.get(Integer.valueOf(purpose));
        } catch(Exception e) {
            s = null;
        }
        if ( s!=null && s.length()>0 ) {
            return s;
        }
        // Special handling of these two key purposes, because if they do not exist, very strange things can happen 
        // if we claim that our "defaultKey" is the previous or next signing key, when it in fact is not.
        if ((purpose != CATokenConstants.CAKEYPURPOSE_CERTSIGN_PREVIOUS) && (purpose != CATokenConstants.CAKEYPURPOSE_CERTSIGN_NEXT)) {
        	return defaultKeyS;
        }
        return null;
    }
    /** Returns which property key is used for a certain key purpose. 
     * For example for CryptoTokenConstants.CAKEYPURPOSE_CERTSIGN would either CAKEYPURPOSE_CERTSIGN_STRING (certSignKey) 
     * or CAKEYPURPOSE_DEFAULT_STRING (defaultKey) be returned.
     * Special handling is for CERTSIGN_PREVIOUS and CERTSIGN_NEXT. If they can not be found, the defaultKey is _not_ returned.
     */ 
    public String getPurposeProperty(final int purpose) {
        String s;
        try {
            s = (String)keymap.get(Integer.valueOf(purpose));
        } catch(Exception e) {
            s = null;
        }
        if ( s!=null && s.length()>0 ) {
            return s;
        }
        // Special handling of these two key purposes, because if they do not exist, very strange things can happen 
        // if we claim that our "defaultKey" is the previous or next signing key, when it in fact is not.
        if ((purpose != CATokenConstants.CAKEYPURPOSE_CERTSIGN_PREVIOUS) && (purpose != CATokenConstants.CAKEYPURPOSE_CERTSIGN_NEXT)) {
        	return CATokenConstants.CAKEYPURPOSE_DEFAULT_STRING;
        }
        return null;
    }
    
    /** Returns an array with all key aliases that have been registered in this mapping.
     * 
     * @return String[] with key aliases
     */
    public String[] getAliases() {
        Set<String> set = new HashSet<String>();
        set.addAll(map.values());
        if(defaultKeyS != null){
          set.add(defaultKeyS);
        }
        return (String[])set.toArray(new String[set.size()]);
    }
    
    public String toString() {
    	return map.toString();
    }
}
