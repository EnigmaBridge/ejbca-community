package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.util.DnComponents;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.util.passgen.PasswordGeneratorFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Initialized profiles required for VPN operation.
 *
 * Created by dusanklinec on 10.01.17.
 */
public class InitEntityProfilesCommand extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(InitEntityProfilesCommand.class);

    @Override
    public String getMainCommand() {
        return "initprofiles";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        final EndEntityType endEntityType = EndEntityTypes.ENDUSER.toEndEntityType();

        StringBuilder errorString = new StringBuilder();

        // Test if CA exists
        CAInfo vpnCA = null;
        try {
            vpnCA = this.getVpnCA();
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        // Test if end entity profile exists
        boolean createClientProfile = false;
        int endEntityProfile = -1;
        try {
            endEntityProfile = this.getVpnClientEndEntityProfile();
        } catch (EndEntityProfileNotFoundException e) {
            createClientProfile = true;
        }

        if (!createClientProfile){
            log.info("End entity exists");
            //TODO: remove return CommandResult.SUCCESS;
        }

        // Create end entity profile.
        final EndEntityProfileSessionRemote remote = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
        final EndEntityProfile profile = new EndEntityProfile();

        // Default CA & Available CAs
        final String vpnCaId = Integer.toString(vpnCA.getCAId());
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
        profile.setUse(EndEntityProfile.AUTOGENPASSWORDTYPE, 0, true);

        // Cert settings
        profile.setRequired(DnComponents.COMMONNAME,0,true);
        profile.setUse(EndEntityProfile.EMAIL, 0, true);
        profile.addField(DnComponents.RFC822NAME);
        profile.setUse(DnComponents.RFC822NAME, 0, true);

        profile.setAllowMergeDnWebServices(false);
        profile.setAvailableCertificateProfileIds(Collections.singletonList(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER));

        try {
            final String profileName = "VPN_"+System.currentTimeMillis(); //VpnCons.DEFAULT_END_ENTITY_PROFILE; // TODO: parametrise.
            remote.addEndEntityProfile(getAuthenticationToken(), profileName, profile);
            log.info(String.format("End entity [%s] created", profileName));

        } catch (EndEntityProfileExistsException e) {
            log.error("Ent entity profile already exists");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to add end entity profile.");
            return CommandResult.AUTHORIZATION_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public String getCommandDescription() {
        return "Initializes VPN end entity profiles required for operation";
    }

    @Override
    public String getFullHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription() + "\n\n");
        sb.append("Please note VPN CA has to be already created when calling this.\n\n");
        String existingCas = "";

        // Get existing CAs
        Collection<Integer> cas = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getAuthorizedCaIds(getAuthenticationToken());
        try {
            for (int caid : cas) {
                CAInfo info = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getCAInfo(getAuthenticationToken(), caid);
                existingCas += (existingCas.length() == 0 ? "" : ", ") + "\"" + info.getName() + "\"";
            }
        } catch (AuthorizationDeniedException e) {
            existingCas = "ERROR: CLI user not authorized to fetch available CAs>";
        } catch (CADoesntExistsException e) {
            throw new IllegalStateException("CA couldn't be retrieved even though it was just referenced.");
        }
        sb.append("Existing CAs: " + existingCas + "\n");

        // Get End entity profiles
        String endEntityProfiles = "";
        Collection<Integer> eps = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getAuthorizedEndEntityProfileIds(
                getAuthenticationToken());
        for (int epid : eps) {
            endEntityProfiles += (endEntityProfiles.length() == 0 ? "" : ", ") + "\""
                    + EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getEndEntityProfileName(epid) + "\"";
        }
        sb.append("End entity profiles: " + endEntityProfiles + "\n");

        // Get Cert profiles
        String certificateProfiles = "";
        Collection<Integer> cps = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class)
                .getAuthorizedCertificateProfileIds(getAuthenticationToken(), CertificateConstants.CERTTYPE_ENDENTITY);
        for (int cpid : cps) {
            certificateProfiles += (certificateProfiles.length() == 0 ? "" : ", ") + "\""
                    + EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getCertificateProfileName(cpid) + "\"";
        }
        sb.append("Certificate profiles: " + certificateProfiles + "\n\n");
        sb.append("If an End entity profile is selected it must allow selected Certificate profiles.\n");
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
