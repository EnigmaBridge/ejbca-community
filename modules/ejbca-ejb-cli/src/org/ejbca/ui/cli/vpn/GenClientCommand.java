package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityExistsException;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;

/**
 * Adds a new VPN user / generates a new key set for the client.
 *
 * @author ph4r05
 * Created by dusanklinec on 11.01.17.
 */
public class GenClientCommand extends BaseVpnCommand {
    private static final Logger log = Logger.getLogger(GenClientCommand.class);

    private static final String DIRECTORY_KEY = "--directory";
    private static final String EMAIL_KEY = "--email";
    private static final String DEVICE_KEY = "--device";
    private static final String REGENERATE_KEY = "--regenerate";
    private static final String PASSWORD_KEY = "--password";
    private static final String PEM_KEY = "--pem";
    private static final String VPN_CONFIG_KEY = "--vpn";
    private static final String VPN_SUPERADMIN_KEY = "--superadmin";

    {
        registerParameter(new Parameter(DIRECTORY_KEY, "Directory", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "The name of the directory to store the keys to. If not specified, the user keys are not exported."));
        registerParameter(new Parameter(EMAIL_KEY, "Email", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Email address of the client to add/generate"));
        registerParameter(new Parameter(DEVICE_KEY, "Device", MandatoryMode.MANDATORY, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Device identifier of the user to add/generate"));
        registerParameter(new Parameter(REGENERATE_KEY, "Regenerate", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, existing user is revoked & regenerated."));
        registerParameter(new Parameter(PASSWORD_KEY, "Password", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Password for the new end entity key store. Will be prompted for if not set."));
        registerParameter(new Parameter(PEM_KEY, "PEM", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, PEM files are dumped together with P12."));
        registerParameter(new Parameter(VPN_CONFIG_KEY, "VpnConfig", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, OpenVPN config files are dumped together with P12."));
        registerParameter(new Parameter(VPN_SUPERADMIN_KEY, "Superadmin", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.FLAG,
                "If parameter is used, the user is given superadmin role"));
    }

    private String mainStoreDir;

    @Override
    public String getMainCommand() {
        return "genclient";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        log.trace(">execute()");

        CryptoProviderTools.installBCProvider();
        StringBuilder errorString = new StringBuilder();
        final String argDirectory = parameters.get(DIRECTORY_KEY);
        final String argEmail = parameters.get(EMAIL_KEY);
        final String argDevice = parameters.get(DEVICE_KEY);
        final boolean shouldRegenerate = (parameters.get(REGENERATE_KEY) != null);
        final boolean genPem = (parameters.get(PEM_KEY) != null);
        final boolean genVpnConfig = (parameters.get(VPN_CONFIG_KEY) != null);
        final boolean isSuperadmin = (parameters.get(VPN_SUPERADMIN_KEY) != null);

        // Email validity test & device non-nullity test.
        if (!isEmailAndDeviceValid(argEmail, argDevice)){
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        // Test if CA exists
        // Test if the server end entity profile exists.
        try {
            final CAInfo vpnCA = this.getVpnCA();
            final int endEntityClientProfile = this.getVpnClientEndEntityProfile();
            final int certProfileId = CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER;
            final VpnUser tplUser = new VpnUser();
            tplUser.setEmail(argEmail);
            tplUser.setDevice(argDevice);
            if (isSuperadmin){
                tplUser.setAdminRole(VpnCons.ROLE_SUPERADMIN);
            }

            final String userName = VpnUtils.getUserName(tplUser);

            // Key & config export directory.
            if (argDirectory != null && !argDirectory.isEmpty()){
                final File dir = new File(argDirectory).getCanonicalFile();
                dir.mkdirs();
                mainStoreDir = dir.getCanonicalPath();
            }

            // Load user with specified user name.
            EndEntityInformation uservo = getRemoteSession(EndEntityAccessSessionRemote.class)
                    .findUser(getAuthenticationToken(), userName);
            VpnUser vpnUser = null;

            if (uservo != null && !shouldRegenerate){
                log.error(String.format("User with user name %s already exists. " +
                        "Use %s flag to revoke & generate new credentials for existing users", userName, REGENERATE_KEY));
                return CommandResult.FUNCTIONAL_FAILURE;
            }

            // User does not exist -> create a new one.
            if (uservo == null){
                uservo = VpnUserHelper.newEndEntity(tplUser, vpnCA.getCAId(), endEntityClientProfile, certProfileId);

                // The new auto-generated password is generated now, stored to uservo end entity.
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .addUser(getAuthenticationToken(), uservo, false);

                // Create user itself
                try {
                    vpnUser = getRemoteSession(VpnUserManagementSessionRemote.class)
                            .createVpnUser(getAuthenticationToken(), tplUser);

                } catch(Exception e){
                    getRemoteSession(EndEntityManagementSessionRemote.class)
                            .revokeAndDeleteUser(getAuthenticationToken(), uservo.getUsername(), 0);
                    throw new Exception("Exception in creating a new VPN user", e);
                }

            } else {
                // Load user & entity
                vpnUser = getRemoteSession(VpnUserManagementSessionRemote.class)
                        .getVpnUser(getAuthenticationToken(), tplUser.getEmail(), tplUser.getDevice());

                // Admin role update
                vpnUser.setAdminRole(isSuperadmin ? VpnCons.ROLE_SUPERADMIN : null);
                getRemoteSession(VpnUserManagementSessionRemote.class)
                        .saveVpnUser(getAuthenticationToken(), vpnUser);

                // Revoke existing certificate
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .revokeUser(getAuthenticationToken(), uservo.getUsername(), 0);

                // Update password.
                uservo.setPassword(VpnUtils.genRandomPwd());
                uservo.setTimeModified(new Date());
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .changeUser(getAuthenticationToken(), uservo, false);

                // Set status to new
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .setUserStatus(getAuthenticationToken(), uservo.getUsername(),
                                EndEntityConstants.STATUS_NEW);

                getRemoteSession(VpnUserManagementSessionRemote.class)
                        .revokeVpnUser(getAuthenticationToken(), vpnUser.getId());

                // CRL
                checkCrl(null, null);
            }

            try {
                // Create certificate
                vpnUser = getRemoteSession(VpnUserManagementSessionRemote.class)
                        .newVpnCredentials(getAuthenticationToken(), vpnUser.getId(),
                                OptionalNull.ofNullable(uservo.getPassword()), null);

                // Store key store + config?
                if (mainStoreDir != null){
                    final String password = getAuthenticationCode(parameters.get(PASSWORD_KEY));
                    storeKeyStore(vpnUser, password, genPem, genVpnConfig);
                }

                // Send an email.
                getRemoteSession(VpnUserManagementSessionRemote.class)
                        .sendConfigurationEmail(getAuthenticationToken(), vpnUser.getId(), null);

                log.info(String.format("User %s created with id %s", VpnUtils.getUserName(vpnUser), vpnUser.getId()));

            } catch (Exception e) {
                // If things went wrong set status to FAILED
                getRemoteSession(EndEntityManagementSessionRemote.class)
                        .setUserStatus(getAuthenticationToken(), uservo.getUsername(), EndEntityConstants.STATUS_FAILED);

                log.error(InternalEjbcaResources.getInstance().getLocalizedMessage(
                        "vpn.errorsetstatus", "FAILED"), e);

                return CommandResult.FUNCTIONAL_FAILURE;
            }

        } catch (EndEntityProfileNotFoundException e) {
            log.error("ERROR: VPN CA does not exist", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to manage load VPN CA.");
            return CommandResult.AUTHORIZATION_FAILURE;
        } catch (CADoesntExistsException e) {
            log.error("ERROR: VPN CA does not exist");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (IOException e) {
            log.error("ERROR: IO Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (EjbcaException e) {
            log.error("ERROR: EjbcaException Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (EndEntityExistsException e) {
            log.error("ERROR: EndEntityExistsException Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (UserDoesntFullfillEndEntityProfile e) {
            log.error("ERROR: UserDoesntFullfillEndEntityProfile Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (WaitingForApprovalException e) {
            log.error("ERROR: UserDoesntFullfillEndEntityProfile Exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (Exception e) {
            log.error("ERROR: Generic exception", e);
            return CommandResult.FUNCTIONAL_FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    /**
     * Stores keystore.
     *
     * @param vpnUser
     *            VpnUser to store
     * @throws IOException
     *             if directory to store keystore cannot be created
     */
    private void storeKeyStore(VpnUser vpnUser, String kspassword, boolean storePem, boolean storeVpnConfig) throws IOException,
            KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, AuthorizationDeniedException, CADoesntExistsException {

        final String userName = VpnUtils.getUserName(vpnUser);
        if (log.isTraceEnabled()) {
            log.trace(">storeKeyStore: user=" + userName + ", id=" + vpnUser.getId());
        }
        // Where to store it?
        if (mainStoreDir == null) {
            throw new IOException("Can't find directory to store keystore in.");
        }

        final String keyStoreFilenameBase = mainStoreDir + "/" + VpnUtils.sanitizeFileName(userName);
        final String keyStoreFilename = keyStoreFilenameBase + ".p12";
        final FileOutputStream os = new FileOutputStream(keyStoreFilename);

        final KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        final byte[] ksDecoded = Base64.decode(vpnUser.getKeyStore());
        ks.load(new ByteArrayInputStream(ksDecoded), VpnConfig.getKeyStorePass().toCharArray());
        ks.store(os, kspassword.toCharArray());

        log.info("Keystore stored in " + keyStoreFilename);

        // PEM + P12
        if (storePem) {
            final P12toPEM p12topem = new P12toPEM(ks, kspassword, true);
            p12topem.setExportPath(mainStoreDir);
            p12topem.setFileNameBase(VpnUtils.sanitizeFileName(userName));
            p12topem.createPEM();
        }

        // Vpn Configuration
        if (storeVpnConfig) {
            final String vpnConfigName = keyStoreFilenameBase + ".ovpn";
            final String vpnConfig = getRemoteSession(VpnUserManagementSessionRemote.class)
                    .generateVpnConfig(getAuthenticationToken(), vpnUser, null);

            if (vpnConfig == null){
                log.error("Could not generate VPN file");

            } else {
                final File vpnConfigFile = new File(vpnConfigName);
                VpnUtils.readOwnerOnly(vpnConfigFile);

                final BufferedOutputStream ovpnOs = new BufferedOutputStream(new FileOutputStream(vpnConfigFile));
                VpnUtils.readOwnerOnly(new File(vpnConfigFile.getAbsolutePath()));

                ovpnOs.write(vpnConfig.getBytes("UTF-8"));
                ovpnOs.flush();
                ovpnOs.close();
                VpnUtils.readOwnerOnly(new File(vpnConfigFile.getAbsolutePath()));
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("<storeKeyStore: user=" + userName + ", id=" + vpnUser.getId());
        }
    }

    @Override
    public String getCommandDescription() {
        return "Generates a new VPN Client certificate";
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

