package org.ejbca.core.ejb.vpn;

import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.util.DnComponents;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.util.passgen.PasswordGeneratorFactory;

import java.util.Collections;

/**
 * VPN profiles (end entities, certificates) related methods.
 *
 * @author ph4r05
 * Created by dusanklinec on 10.01.17.
 */
public class VpnProfiles {

    /**
     * Returns default template for client end entity profile
     *
     * @param vpnCA CA for VPN to use - only allowed in the profile.
     * @return client end entity profile.
     */
    public static EndEntityProfile getDefaultClientEndEntityProfile(int vpnCA){
        final EndEntityProfile profile = new EndEntityProfile();

        // Default CA & Available CAs
        final String vpnCaId = Integer.toString(vpnCA);
        profile.setValue(EndEntityProfile.AVAILCAS, 0,vpnCaId);
        profile.setRequired(EndEntityProfile.AVAILCAS, 0,true);
        profile.setValue(EndEntityProfile.DEFAULTCA, 0, vpnCaId);
        profile.setRequired(EndEntityProfile.DEFAULTCA, 0,true);

        // Key Stores
        profile.setValue(EndEntityProfile.DEFKEYSTORE,0, Integer.toString(SecConst.TOKEN_SOFT_BROWSERGEN));

        // Passwords
        profile.setRequired(EndEntityProfile.PASSWORD,0,false);
        profile.setUse(EndEntityProfile.PASSWORD,0 ,false);
        profile.setModifyable(EndEntityProfile.PASSWORD,0 ,true);
        profile.setValue(EndEntityProfile.AUTOGENPASSWORDTYPE,0, PasswordGeneratorFactory.PASSWORDTYPE_NOTALIKEENLD);
        profile.setValue(EndEntityProfile.AUTOGENPASSWORDLENGTH, 0, "16");
        profile.setUse(EndEntityProfile.AUTOGENPASSWORDTYPE, 0, false);

        // Cert settings
        profile.setRequired(DnComponents.COMMONNAME,0,true);
        profile.setUse(EndEntityProfile.EMAIL, 0, true);
        profile.addField(DnComponents.RFC822NAME);
        profile.setUse(DnComponents.RFC822NAME, 0, true);

        profile.setAllowMergeDnWebServices(false);
        profile.setAvailableCertificateProfileIds(Collections.singletonList(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER));
        return profile;
    }

    /**
     * Returns default template for server end entity profile
     *
     * @param vpnCA CA for VPN to use - only allowed in the profile.
     * @return server end entity profile.
     */
    public static EndEntityProfile getDefaultServerEndEntityProfile(int vpnCA){
        final EndEntityProfile profile = new EndEntityProfile();

        // Default CA & Available CAs - bound to VPN CA.
        final String vpnCaId = Integer.toString(vpnCA);
        profile.setValue(EndEntityProfile.AVAILCAS, 0,vpnCaId);
        profile.setRequired(EndEntityProfile.AVAILCAS, 0,true);
        profile.setValue(EndEntityProfile.DEFAULTCA, 0, vpnCaId);
        profile.setRequired(EndEntityProfile.DEFAULTCA, 0,true);

        // Key Stores - all available, up to admin to choose the suitable one.
        profile.setValue(EndEntityProfile.DEFKEYSTORE,0, Integer.toString(SecConst.TOKEN_SOFT_BROWSERGEN));

        // Passwords - use password for the server certificate, defined by the admin.
        profile.setRequired(EndEntityProfile.PASSWORD,0,true);
        profile.setUse(EndEntityProfile.PASSWORD,0 ,true);
        profile.setModifyable(EndEntityProfile.PASSWORD,0 ,true);
        profile.setValue(EndEntityProfile.AUTOGENPASSWORDTYPE,0, PasswordGeneratorFactory.PASSWORDTYPE_NOTALIKEENLD);
        profile.setValue(EndEntityProfile.AUTOGENPASSWORDLENGTH, 0, "16");
        profile.setUse(EndEntityProfile.AUTOGENPASSWORDTYPE, 0, false);

        // Cert settings
        profile.setRequired(DnComponents.COMMONNAME,0,true);
        profile.setUse(EndEntityProfile.EMAIL, 0, false);
        profile.removeField(DnComponents.RFC822NAME, 0);
        profile.addField(DnComponents.ORGANIZATIONALUNIT);
        profile.setRequired(DnComponents.ORGANIZATIONALUNIT, 0, false);

        profile.setAllowMergeDnWebServices(false);
        profile.setAvailableCertificateProfileIds(Collections.singletonList(CertificateProfileConstants.CERTPROFILE_FIXED_SERVER));
        return profile;
    }


}
