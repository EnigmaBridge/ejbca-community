package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ra.EndEntityExistsException;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

import java.util.Date;

/**
 * Generates VPN Server certificate under VPN CA.
 *
 * @author ph4r05
 * Created by dusanklinec on 11.01.17.
 */
public class GenServerCertCommand  extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(InitEntityProfilesCommand.class);

    private static final String PASSWORD_KEY = "--password";

    {
        registerParameter(new Parameter(PASSWORD_KEY, "Password", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Password for the new end entity. Will be prompted for if not set."));
    }

    @Override
    public String getMainCommand() {
        return "genserver";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        StringBuilder errorString = new StringBuilder();
        final String password = getAuthenticationCode(parameters.get(PASSWORD_KEY));

        // Test if CA exists
        // Test if the server end entity profile exists.
        try {
            // 1. Create a new End Entity
            final int endProfileId = getVpnServerEndEntityProfile();
            final int certProfileId = CertificateProfileConstants.CERTPROFILE_FIXED_SERVER;
            final CAInfo vpnCA = getVpnCA();
            final String cn = CertTools.getPartFromDN(vpnCA.getSubjectDN(), "CN");
            final EndEntityInformation uservo = new EndEntityInformation(
                    VpnCons.VPN_SERVER_USERNAME,
                    String.format("CN=%s,UO=VPN", cn),
                    vpnCA.getCAId(),
                    null,null,
                    EndEntityConstants.STATUS_NEW,
                    new EndEntityType(EndEntityTypes.ENDUSER),
                    endProfileId,
                    certProfileId,
                    new Date(), new Date(),
                    SecConst.TOKEN_SOFT_P12,
                    0, null);
            uservo.setPassword(password);

            try {
                EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class)
                        .addUser(getAuthenticationToken(), uservo, false);

            } catch (UserDoesntFullfillEndEntityProfile userDoesntFullfillEndEntityProfile) {
                log.error("Problem with the end entity profile for VPN server", userDoesntFullfillEndEntityProfile);
                return CommandResult.FUNCTIONAL_FAILURE;

            } catch (EndEntityExistsException e) {
                log.error("Server entity already exists");
                return CommandResult.FUNCTIONAL_FAILURE;

            } catch (WaitingForApprovalException e) {
                log.error("This operation needs approval");
                return CommandResult.FUNCTIONAL_FAILURE;

            } catch (EjbcaException e) {
                log.error("Generic exception in creating server end entity", e);
                return CommandResult.FUNCTIONAL_FAILURE;
            }

            // TODO:
            // 2. Generate a new keypair
            // 3. Generate a new certificate
            // 4. Export as PEMs to the PEM directoryp


        } catch (EndEntityProfileNotFoundException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        }



        return CommandResult.SUCCESS;
    }

    @Override
    public String getCommandDescription() {
        return "Generates a new VPN Server certificate";
    }

    @Override
    public String getFullHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription() + "\n\n");
        sb.append("Please note VPN CA has to be already created when calling this.\n\n");
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}

