package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileExistsException;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.vpn.VpnConfig;
import org.ejbca.core.ejb.vpn.VpnProfiles;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;

/**
 * Initialized profiles required for VPN operation.
 *
 * @author ph4r05
 * Created by dusanklinec on 10.01.17.
 */
public class InitProfilesCommand extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(InitProfilesCommand.class);

    private CAInfo vpnCA;
    private Integer serverCertProfileId;
    private Integer clientCertProfileId;

    private Integer clientEndEntityProfileId;
    private Integer serverEndEntityProfileId;

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
        try {
            vpnCA = this.getVpnCA();
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        try {
            this.createServerCertProfile();
            this.createClientCertProfile();
            this.createServerEntityProfile();
            this.createClientEntityProfile();
        } catch (EndEntityProfileExistsException e) {
            log.error("End entity profile already exists");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (CertificateProfileExistsException e) {
            log.error("Certificate profile already exists");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to add end entity profile.");
            return CommandResult.AUTHORIZATION_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    /**
     * Creates server end entity profile
     * @throws EndEntityProfileExistsException
     * @throws AuthorizationDeniedException
     */
    private void createServerEntityProfile() throws EndEntityProfileExistsException, AuthorizationDeniedException {
        final String profileName = VpnConfig.getServerEndEntityProfile();
        serverEndEntityProfileId = tryGetEntityProfileId(profileName);

        if (serverEndEntityProfileId != null){
            log.info("Server entity profile exists: " + serverEndEntityProfileId);
            return;
        }

        // Create end entity profile - client
        final EndEntityProfileSessionRemote remote = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
        final EndEntityProfile profile = VpnProfiles.getDefaultServerEndEntityProfile(vpnCA.getCAId(), serverCertProfileId);
        remote.addEndEntityProfile(getAuthenticationToken(), profileName, profile);
        log.info(String.format("End entity for server: %s created", profileName));
    }

    /**
     * Creates client end entity profile
     * @throws EndEntityProfileExistsException
     * @throws AuthorizationDeniedException
     */
    private void createClientEntityProfile() throws EndEntityProfileExistsException, AuthorizationDeniedException {
        final String profileName = VpnConfig.getClientEndEntityProfile();
        clientEndEntityProfileId = tryGetEntityProfileId(profileName);

        if (clientEndEntityProfileId != null){
            log.info("Client entity profile exists: " + clientEndEntityProfileId);
            return;
        }

        // Create end entity profile - client
        final EndEntityProfileSessionRemote remote = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
        final EndEntityProfile profile = VpnProfiles.getDefaultClientEndEntityProfile(vpnCA.getCAId(), clientCertProfileId);
        remote.addEndEntityProfile(getAuthenticationToken(), profileName, profile);
        log.info(String.format("End entity for client: %s created", profileName));
    }

    /**
     * Creates server certificate profile if needed.
     */
    private void createServerCertProfile() throws CertificateProfileExistsException, AuthorizationDeniedException {
        serverCertProfileId = this.getVpnServerCertificateProfile();
        if (serverCertProfileId != 0){
            log.info("Server certificate profile exists: " + serverCertProfileId);
            return;
        }

        serverCertProfileId = addCertProfile(
                VpnConfig.getVpnServerCertificateProfile(),
                VpnProfiles.getDefaultServerCertProfile(vpnCA.getCAId()));
        log.info("Server certificate profile created: " + serverCertProfileId);
    }

    /**
     * Creates client certificate profile if needed.
     */
    private void createClientCertProfile() throws CertificateProfileExistsException, AuthorizationDeniedException {
        clientCertProfileId = this.getVpnClientCertificateProfile();
        if (clientCertProfileId != 0){
            log.info("Client certificate profile exists: " + clientCertProfileId);
            return;
        }

        clientCertProfileId = addCertProfile(
                VpnConfig.getVpnClientCertificateProfile(),
                VpnProfiles.getDefaultClientCertProfile(vpnCA.getCAId()));
        log.info("Client certificate profile created: " + clientCertProfileId);
    }

    /**
     * Tries to load profile ID, returns null if does not exist
     * @param profileName profile name to load ID for
     * @return id or null
     */
    private Integer tryGetEntityProfileId(String profileName){
        try {
            return EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class)
                    .getEndEntityProfileId(profileName);
        } catch (EndEntityProfileNotFoundException e) {
            return null;
        }
    }

    /**
     * Adds a new certificate profile.
     * @param name name of the profile to add
     * @param profile certificate profile to add
     * @return cert profile id
     * @throws CertificateProfileExistsException
     * @throws AuthorizationDeniedException
     */
    private int addCertProfile(String name, CertificateProfile profile) throws CertificateProfileExistsException, AuthorizationDeniedException {
        final CertificateProfileSessionRemote rcert = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class);
        return rcert.addCertificateProfile(getAuthenticationToken(), name, profile);
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

        // Add existing CAs, end entity profiles, certificate profiles
        addAvailableStuff(sb);

        sb.append("\nIf an End entity profile is selected it must allow selected Certificate profiles.\n");
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
