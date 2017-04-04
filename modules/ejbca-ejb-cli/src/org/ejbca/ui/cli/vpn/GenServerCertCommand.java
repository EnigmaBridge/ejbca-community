package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityExistsException;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;

/**
 * Generates VPN Server certificate under VPN CA.
 *
 * @author ph4r05
 * Created by dusanklinec on 11.01.17.
 */
public class GenServerCertCommand extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(GenServerCertCommand.class);

    private static final String PASSWORD_KEY = "--password";
    private static final String DIRECTORY_KEY = "--directory";
    private static final String PEM_KEY = "--pem";
    private static final String REGENERATE_KEY = "--regenerate";
    private static final String CREATE_KEY = "--create";

    {
        registerParameter(new Parameter(PASSWORD_KEY, "Password", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Password for the new end entity. Will be prompted for if not set."));
        registerParameter(new Parameter(DIRECTORY_KEY, "Directory", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "The name of the directory to store the keys to. If not specified, the current EJBCA_HOME/"+VpnCons.VPN_DATA+" directory will be used."));
        registerParameter(new Parameter(PEM_KEY, "PEM", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, PEM files are dumped together with P12."));
        registerParameter(new Parameter(REGENERATE_KEY, "Regenerate", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, existing server is revoked & regenerated."));
        registerParameter(new Parameter(CREATE_KEY, "Create", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, server end entity is created if missing."));
    }

    private String mainStoreDir;

    @Override
    public String getMainCommand() {
        return "genserver";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        final String argDirectory = parameters.get(DIRECTORY_KEY);
        final boolean genPem = (parameters.get(PEM_KEY) != null);
        final boolean regenerate = (parameters.get(REGENERATE_KEY) != null);
        final boolean createIfMissing = (parameters.get(REGENERATE_KEY) != null);
        final String password = getAuthenticationCode(parameters.get(PASSWORD_KEY));

        StringBuilder errorString = new StringBuilder();
        try {
            // Dir if does not exist.
            final File vpnDataDir = VpnConfig.getVpnDataDir(argDirectory);
            setMainStoreDir(vpnDataDir.getAbsolutePath());

            // 1. Create a new End Entity
            final int endProfileId = getVpnServerEndEntityProfile();
            final int certProfileId = CertificateProfileConstants.CERTPROFILE_FIXED_SERVER;
            final CAInfo vpnCA = getVpnCA();
            final String cn = CertTools.getPartFromDN(vpnCA.getSubjectDN(), "CN");
            EndEntityInformation uservo = null;

            // Regenerate ? Fetch user entity.
            try {
                if (regenerate){
                    uservo = getRemoteSession(EndEntityAccessSessionRemote.class)
                            .findUser(getAuthenticationToken(), VpnCons.VPN_SERVER_USERNAME);

                    if (uservo == null && !createIfMissing){
                        log.error(String.format("Server end entity [%s] does not exist", VpnCons.VPN_SERVER_USERNAME));
                        return CommandResult.FUNCTIONAL_FAILURE;

                    } else if (uservo != null) {
                        // Revoke existing certificate
                        getRemoteSession(EndEntityManagementSessionRemote.class)
                                .revokeUser(getAuthenticationToken(), uservo.getUsername(), 0);

                        // Update password.
                        uservo.setPassword(password);
                        uservo.setTimeModified(new Date());
                        getRemoteSession(EndEntityManagementSessionRemote.class)
                                .changeUser(getAuthenticationToken(), uservo, false);

                        // Set status to new
                        getRemoteSession(EndEntityManagementSessionRemote.class)
                                .setUserStatus(getAuthenticationToken(), uservo.getUsername(),
                                        EndEntityConstants.STATUS_NEW);

                        // CRL
                        checkCrl(null, null);
                    }
                }

                // User is null - create a new one
                if (uservo == null) {
                    uservo = new EndEntityInformation(
                            VpnCons.VPN_SERVER_USERNAME,
                            CertTools.stringToBCDNString(String.format("CN=%s,OU=VPN", cn)),
                            vpnCA.getCAId(),
                            null, null,
                            EndEntityConstants.STATUS_NEW,
                            EndEntityTypes.ENDUSER.toEndEntityType(),
                            endProfileId,
                            certProfileId,
                            new Date(), new Date(),
                            SecConst.TOKEN_SOFT_P12,
                            0, null);
                    uservo.setPassword(password);

                    getRemoteSession(EndEntityManagementSessionRemote.class)
                            .addUser(getAuthenticationToken(), uservo, false);
                }

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

            // 2. Generate a new key-pair
            // 3. Generate a new certificate
            final VpnCertGenerator generator = new VpnCertGenerator();
            generator.setFetchRemoteSessions(true);
            generator.setAuthenticationTokenProvider(new AuthenticationTokenProvider() {
                @Override
                public AuthenticationToken getAuthenticationToken() {
                    return GenServerCertCommand.this.getAuthenticationToken();
                }
            });

            final KeyStore ks = generator.generateClient(uservo, false, false, false,
                    3660L);

            // If all was OK, users status is set to GENERATED by the
            // signsession when the user certificate is created.
            // If status is still NEW, FAILED or KEYRECOVER though, it means we
            // should set it back to what it was before, probably it had a equest counter
            // meaning that we should not reset the clear text password yet.
            EndEntityInformation vo = getRemoteSession(EndEntityAccessSessionRemote.class)
                    .findUser(getAuthenticationToken(), uservo.getUsername());

            if ((vo.getStatus() == EndEntityConstants.STATUS_NEW) || (vo.getStatus() == EndEntityConstants.STATUS_FAILED)
                    || (vo.getStatus() == EndEntityConstants.STATUS_KEYRECOVERY)) {
                getRemoteSession(EndEntityManagementSessionRemote.class).
                        setClearTextPassword(getAuthenticationToken(), uservo.getUsername(), uservo.getPassword());
            } else {
                // Delete clear text password, if we are not letting status be
                // the same as originally
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .setClearTextPassword(getAuthenticationToken(), uservo.getUsername(), null);
            }

            String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.generateduser", uservo.getUsername());
            log.info(iMsg);

            // 4. Export as PEMs to the PEM directory
            storeKeyStore(ks, uservo.getUsername(), password, genPem);

        } catch (EndEntityProfileNotFoundException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.", e);
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthLoginException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.", e);
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (IOException e) {
            log.error("ERROR: IO error", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (Exception e) {
            log.error("ERROR: General exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    /**
     * Sets the location where generated VPN files will be stored, full name
     * will be: mainStoreDir/username.p12.
     *
     * @param dir
     *            existing directory
     */
    private void setMainStoreDir(String dir) {
        mainStoreDir = dir;
    }

    /**
     * Stores keystore.
     *
     * @param ks
     *            KeyStore
     * @param username
     *            username, the owner of the keystore
     * @param kspassword
     *            the password used to protect the peystore
     * @throws IOException
     *             if directory to store keystore cannot be created
     */
    private void storeKeyStore(KeyStore ks, String username, String kspassword, boolean storePem) throws IOException,
            KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException {
        if (log.isTraceEnabled()) {
            log.trace(">storeKeyStore: ks=" + ks.toString() + ", username=" + username);
        }
        // Where to store it?
        if (mainStoreDir == null) {
            throw new IOException("Can't find directory to store keystore in.");
        }

        String keyStoreFilenameBase = mainStoreDir + "/" + VpnUtils.sanitizeFileName(username);
        String keyStoreFilename = keyStoreFilenameBase + ".p12";

        FileOutputStream os = new FileOutputStream(keyStoreFilename);
        ks.store(os, kspassword.toCharArray());
        log.info("Keystore stored in " + keyStoreFilename);

        // PEM + P12
        if (storePem) {
            final P12toPEM p12topem = new P12toPEM(ks, kspassword, true);
            p12topem.setExportPath(mainStoreDir);
            p12topem.setFileNameBase(VpnUtils.sanitizeFileName(username));
            p12topem.createPEM();
        }

        if (log.isTraceEnabled()) {
            log.trace("<storeKeyStore: ks=" + ks.toString() + ", username=" + username);
        }
    }

    @Override
    public String getCommandDescription() {
        return "Generates a new VPN Server certificate";
    }

    @Override
    public String getFullHelpText() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription()).append("\n\n");
        sb.append("Please note VPN CA has to be already created when calling this.\n\n");
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}

