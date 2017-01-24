/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.ui.cli.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.util.EjbRemoteHelper;
import org.cesecore.util.StringTools;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.vpn.AuthenticationTokenProvider;
import org.ejbca.core.ejb.vpn.VpnConfig;
import org.ejbca.core.ejb.vpn.VpnCrlGenerator;
import org.ejbca.core.ejb.vpn.VpnUtils;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.ui.cli.infrastructure.command.EjbcaCliUserCommandBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base for VPN commands, contains common functions for VPN operation.
 *
 * @author ph4r05
 */
public abstract class BaseVpnCommand extends EjbcaCliUserCommandBase {
    private static final Logger log = Logger.getLogger(BaseVpnCommand.class);

	public static final String MAINCOMMAND = "vpn";
	
    @Override
    public String[] getCommandPath() {
        return new String[] { MAINCOMMAND };
    }

    /**
     * Returns a cached remote session bean.
     *
     * @param key the @Remote-appended interface for this session bean
     * @return the sought interface, or null if it doesn't exist in JNDI context.
     */
    public static <T> T getRemoteSession(final Class<T> key) {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(key);
    }

    /**
     * VPN end entity server profile.
     * @return
     * @throws EndEntityProfileNotFoundException
     */
    protected int getVpnServerEndEntityProfile() throws EndEntityProfileNotFoundException {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class)
                .getEndEntityProfileId(VpnConfig.getServerEndEntityProfile());
    }

    /**
     * VPN end entity client profile.
     * @return
     * @throws EndEntityProfileNotFoundException
     */
    protected int getVpnClientEndEntityProfile() throws EndEntityProfileNotFoundException {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class)
                .getEndEntityProfileId(VpnConfig.getClientEndEntityProfile());
    }

    /**
     * Returns VPN CA.
     * @return
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     */
    protected CAInfo getVpnCA() throws AuthorizationDeniedException, CADoesntExistsException {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                .getCAInfo(getAuthenticationToken(), VpnConfig.getCA());
    }

    /**
     * Returns list of all CAs.
     * @return
     * @throws AuthorizationDeniedException
     */
    protected List<CAInfo> getCAs() throws AuthorizationDeniedException {
        final Collection<Integer> cas = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                .getAuthorizedCaIds(getAuthenticationToken());
        final List<CAInfo> infoList = new ArrayList<>(cas.size());

        try {
            for (int caid : cas) {
                CAInfo info = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                        .getCAInfo(getAuthenticationToken(), caid);
                infoList.add(info);
            }
        } catch (CADoesntExistsException e) {
            throw new IllegalStateException("CA couldn't be retrieved even though it was just referenced.");
        }

        return infoList;
    }

    /**
     * Adds list of available CAs to the string builder (for help)
     * @param sb
     */
    protected void addAvailableCas(StringBuilder sb){
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
    }

    /**
     * Adds list of available end entity profiles to the string builder (for help)
     * @param sb
     */
    protected void addAvailableEndProfiles(StringBuilder sb){
        String endEntityProfiles = "";
        Collection<Integer> eps = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getAuthorizedEndEntityProfileIds(
                getAuthenticationToken());
        for (int epid : eps) {
            endEntityProfiles += (endEntityProfiles.length() == 0 ? "" : ", ") + "\""
                    + EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getEndEntityProfileName(epid) + "\"";
        }
        sb.append("End entity profiles: " + endEntityProfiles + "\n");
    }

    /**
     * Adds list of available certificate profiles to the string builder (for help)
     * @param sb
     */
    protected void addAvailableCertProfiles(StringBuilder sb){
        String certificateProfiles = "";
        Collection<Integer> cps = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class)
                .getAuthorizedCertificateProfileIds(getAuthenticationToken(), CertificateConstants.CERTTYPE_ENDENTITY);
        for (int cpid : cps) {
            certificateProfiles += (certificateProfiles.length() == 0 ? "" : ", ") + "\""
                    + EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getCertificateProfileName(cpid) + "\"";
        }
        sb.append("Certificate profiles: " + certificateProfiles + "\n");
    }

    /**
     * Adds available CAs, profiles to the string builder - for extended help.
     * @param sb string builder to add stuff to
     */
    protected void addAvailableStuff(StringBuilder sb){
        // Get existing CAs
        addAvailableCas(sb);

        // Get End entity profiles
        addAvailableEndProfiles(sb);

        // Get Cert profiles
        addAvailableCertProfiles(sb);
    }

    /**
     * Prompts for the password if not set on command line
     * @param commandLineArgument
     * @return
     */
    protected String getAuthenticationCode(final String commandLineArgument) {
        final String authenticationCode;
        if (commandLineArgument == null || "null".equalsIgnoreCase(commandLineArgument)) {
            getLogger().info("Enter password: ");
            getLogger().info("");
            authenticationCode = StringTools.passwordDecryption(String.valueOf(System.console().readPassword()), "End Entity Password");
        } else {
            authenticationCode = StringTools.passwordDecryption(commandLineArgument, "End Entity Password");
        }
        return authenticationCode;
    }

    /**
     * Returns true if entered parameters are non-empty and have valid format.
     * @param email
     * @param device
     * @return
     */
    protected boolean isEmailAndDeviceValid(String email, String device){
        if (email == null || email.isEmpty()){
            log.error("Email cannot be empty");
            return false;
        }
        if (device == null || device.isEmpty()){
            log.error("Device cannot be empty");
            return false;
        }
        if (!VpnUtils.isEmailValid(email)){
            log.error("Email is invalid");
            return false;
        }

        return true;
    }

    /**
     * Called when some revocation was performed.
     * Generates CRL if the configuration is set so.
     */
    protected void checkCrl(Boolean updateCrl, Boolean updateCrlFile){
        final boolean updateCrlConf = updateCrl != null ? updateCrl : VpnConfig.shouldRefreshCrlOnRevoke();
        final boolean updateCrlFileConf = updateCrlFile != null ? updateCrlFile : VpnConfig.shouldRefreshFileCrlOnRevoke();
        if (!updateCrlConf && !updateCrlFileConf){
            return;
        }

        try {
            final VpnCrlGenerator crlGen = new VpnCrlGenerator();
            crlGen.setWrite(updateCrlFileConf);
            crlGen.setForce(true);
            crlGen.setDer(false);
            crlGen.setCrlDirectory(VpnConfig.getCrlDirectory());
            crlGen.setFetchRemoteSessions(true);
            crlGen.setAuthenticationTokenProvider(new AuthenticationTokenProvider() {
                @Override
                public AuthenticationToken getAuthenticationToken() {
                    return BaseVpnCommand.this.getAuthenticationToken();
                }
            });

            crlGen.generate();
            log.info(String.format("CRL generation. ID: %s, file: %s", crlGen.getCrlId(), crlGen.getCrlPath()));

        } catch(Exception e){
            log.error("Exception in CRL update", e);
        }
    }
}
