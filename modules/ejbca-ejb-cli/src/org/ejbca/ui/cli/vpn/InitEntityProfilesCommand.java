package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
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
                final String profileName = VpnConfig.getClientEndEntityProfile();
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
                final String profileName = VpnConfig.getServerEndEntityProfile();
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
