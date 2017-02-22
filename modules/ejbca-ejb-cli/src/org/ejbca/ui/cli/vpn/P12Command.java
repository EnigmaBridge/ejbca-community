package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.vpn.OtpDownload;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.ejb.vpn.VpnUserManagementSessionRemote;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * P12 command creates a new OTP downloads for p12 files, physically present on the file system
 * Created by dusanklinec on 22.02.17.
 */
public class P12Command  extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(P12Command.class);

    private static final String IDENTIFIER_KEY = "--id";
    private static final String P12_KEY = "--p12";

    {
        registerParameter(new Parameter(IDENTIFIER_KEY, "Identifier", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "User ID to create p12 for"));
        registerParameter(new Parameter(P12_KEY, "p12", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "P12 file to add"));
    }

    @Override
    public String getMainCommand() {
        return "p12";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        final String userId = parameters.get(IDENTIFIER_KEY).trim();
        final String p12 = parameters.get(P12_KEY);

        try {
            final File p12File = new File(p12);
            if (!p12File.exists()) {
                log.error("ERROR: P12 file cannot be found");
                return CommandResult.FUNCTIONAL_FAILURE;
            }

            if (!p12File.canRead()){
                log.error("ERROR: P12 file cannot is not readable");
                return CommandResult.FUNCTIONAL_FAILURE;
            }

            final OtpDownload token = new OtpDownload();
            token.setOtpType(VpnCons.OTP_TYPE_P12);
            token.setOtpId(userId);

            // Raw aux data = json
            final JSONObject aux = new JSONObject();
            aux.put(VpnCons.OTP_AUX_P12_PATH, p12);
            token.setAuxData(aux.toString());

            // Create a new token
            final OtpDownload savedToken = getRemoteSession(VpnUserManagementSessionRemote.class)
                    .otpNew(getAuthenticationToken(), token);

            log.info(String.format("OTP_DOWNLOAD_TOKEN=%s", savedToken.getOtpDownload()));

        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (Exception e) {
            log.error("ERROR: Generic exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public String getCommandDescription() {
        return "Creates new OTP download token for P12 download";
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
