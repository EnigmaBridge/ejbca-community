package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.ejb.vpn.VpnProfiles;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;

import java.util.Collection;

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

        // Test if the client end entity profile exists.
        boolean createClientProfile = false;
        int endEntityProfile = -1;
        try {
            endEntityProfile = this.getVpnClientEndEntityProfile();
        } catch (EndEntityProfileNotFoundException e) {
            createClientProfile = true;
        }

        // Test if the server end entity profile exists.
        boolean createServerProfile = false;
        int endEntityServerProfile = -1;
        try {
            endEntityServerProfile = this.getVpnServerEndEntityProfile();
        } catch (EndEntityProfileNotFoundException e) {
            createServerProfile = true;
        }

        // Client profile - create if needed.
        if (!createClientProfile){
            log.info("End entity for client exists");

        } else {
            // Create end entity profile - client
            final EndEntityProfileSessionRemote remote = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
            final EndEntityProfile profile = VpnProfiles.getDefaultClientEndEntityProfile(vpnCA.getCAId());
            try {
                final String profileName = VpnCons.DEFAULT_END_ENTITY_PROFILE; // TODO: parametrise.
                remote.addEndEntityProfile(getAuthenticationToken(), profileName, profile);
                log.info(String.format("End entity for client: %s created", profileName));

            } catch (EndEntityProfileExistsException e) {
                log.error("Ent entity profile already exists");
                return CommandResult.FUNCTIONAL_FAILURE;
            } catch (AuthorizationDeniedException e) {
                log.error("ERROR: CLI user not authorized to add end entity profile.");
                return CommandResult.AUTHORIZATION_FAILURE;
            }
        }

        // Server profile - create if needed.
        if (!createServerProfile){
            log.info("End entity for server exists");

        } else {
            // Create end entity profile - client
            final EndEntityProfileSessionRemote remote = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
            final EndEntityProfile profile = VpnProfiles.getDefaultServerEndEntityProfile(vpnCA.getCAId());
            try {
                final String profileName = VpnCons.DEFAULT_END_ENTITY_PROFILE_SERVER; // TODO: parametrise.
                remote.addEndEntityProfile(getAuthenticationToken(), profileName, profile);
                log.info(String.format("End entity for client: %s created", profileName));

            } catch (EndEntityProfileExistsException e) {
                log.error("Ent entity profile already exists");
                return CommandResult.FUNCTIONAL_FAILURE;
            } catch (AuthorizationDeniedException e) {
                log.error("ERROR: CLI user not authorized to add end entity profile.");
                return CommandResult.AUTHORIZATION_FAILURE;
            }
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
