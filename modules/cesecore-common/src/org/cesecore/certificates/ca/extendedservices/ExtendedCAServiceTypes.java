package org.cesecore.certificates.ca.extendedservices;

/**
 * 
 * @version $Id: ExtendedCAServiceTypes.java 17742 2013-10-08 11:19:05Z mikekushner $
 *
 */

public class ExtendedCAServiceTypes {

    @Deprecated //Removed in EJBCA 6.0.0, and retained to support migration. Remove once support for upgrading from 4.0.x is dropped. 
	public static final int TYPE_OCSPEXTENDEDSERVICE   = 1; 	
	public static final int TYPE_XKMSEXTENDEDSERVICE   = 2; 
	public static final int TYPE_CMSEXTENDEDSERVICE = 3; 
	public static final int TYPE_HARDTOKENENCEXTENDEDSERVICE = 4;
	public static final int TYPE_KEYRECOVERYEXTENDEDSERVICE = 5;

}
