package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.ejb.vpn.VpnConfig;
import org.ejbca.core.ejb.vpn.VpnUserManagementSessionRemote;
import org.ejbca.core.ejb.vpn.VpnUtils;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Generating new CRLs for the VPN CA.
 *
 * @author ph4r05
 * Created by dusanklinec on 24.01.17.
 */
public class CrlClientCommand  extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(CrlClientCommand.class);

    private static final String DIRECTORY_KEY = "--directory";
    private static final String FORCE_KEY = "--force";
    private static final String DER_KEY = "--der";

    {
        registerParameter(new Parameter(DIRECTORY_KEY, "Directory", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "The name of the directory to store the CRL to. If not specified, default directories are used."));
        registerParameter(new Parameter(FORCE_KEY, "Force", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, the CRL is generated even if the previous one is valid"));
        registerParameter(new Parameter(DER_KEY, "DER", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, CRL is dumped in the DER form."));
    }

    @Override
    public String getMainCommand() {
        return "crl";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        final String argDirectory = parameters.get(DIRECTORY_KEY);
        final boolean force = (parameters.get(FORCE_KEY) != null);
        final boolean der = (parameters.get(DER_KEY) != null);

        // Test if CA exists
        // Test if the server end entity profile exists.
        try {
            final CAInfo vpnCA = this.getVpnCA();

            // Key & config export directory.
            String mainStoreDir;
            if (argDirectory != null && !argDirectory.isEmpty()){
                final File dir = new File(argDirectory).getCanonicalFile();
                dir.mkdirs();
                mainStoreDir = dir.getCanonicalPath();
            } else {
                mainStoreDir = VpnConfig.getCrlDirectory().getCanonicalPath();
            }

            // Generate at first.
            final int crlId = getRemoteSession(VpnUserManagementSessionRemote.class)
                    .generateCRL(getAuthenticationToken(), force, null);
            log.info(String.format("Latest CRL ID: %s", crlId));

            // Get the CRL and store it.
            final byte[] crlData = getRemoteSession(VpnUserManagementSessionRemote.class)
                    .getCRL(getAuthenticationToken());

            if (crlData == null){
                log.error("CRL generation error, current CRL is null");
                return CommandResult.FUNCTIONAL_FAILURE;
            }

            final String cn = CertTools.getPartFromDN(vpnCA.getSubjectDN(), "CN");
            final String crlFileName = VpnUtils.sanitizeFileName(cn + ".crl");
            final File newCrlFile = new File(mainStoreDir, crlFileName);

            final FileOutputStream fos = new FileOutputStream(newCrlFile);
            fos.write(der ? crlData : VpnUtils.crlDerToPem(crlData).getBytes("UTF-8"));
            fos.close();
            log.info(String.format("CRL file generated: %s", newCrlFile.getAbsolutePath()));

        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (IOException e) {
            log.error("ERROR: IO Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (Exception e) {
            log.error("ERROR: Generic exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public String getCommandDescription() {
        return "Generates CRL for VPN CA";
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

