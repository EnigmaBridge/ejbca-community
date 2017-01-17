/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CertTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.util.mail.MailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

/**
 * Management session bean for VPN functionality. Top level.
 *
 * @author ph4r05
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "VpnUserManagement")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class VpnUserManagementSessionBean implements VpnUserManagementSession {

    private static final Logger log = Logger.getLogger(VpnUserManagementSessionBean.class);

    /** Internal localization of logs and errors */
    private static final InternalResources INTRES = InternalResources.getInstance();
    private static final Random rnd = new SecureRandom();

    @EJB
    private AccessControlSessionLocal accessControlSessionSession;
    @EJB
    private SecurityEventsLoggerSessionLocal securityEventsLoggerSession;
    @EJB
    private VpnUserSession vpnUserSession;
    @EJB
    private CaSessionLocal caSession;

    @Override
    public List<Integer> geVpnUsersIds(AuthenticationToken authenticationToken) {
        final List<Integer> allVpnUsersIds = vpnUserSession.getVpnUserIds();
        final List<Integer> authorizedVpnUserIds = new ArrayList<Integer>();
        for (final Integer current : allVpnUsersIds) {
            if (accessControlSessionSession.isAuthorizedNoLogging(authenticationToken,
                    VpnRules.USER_VIEW.resource() + "/" + current.toString())) {
                authorizedVpnUserIds.add(current);
            }
        }
        return authorizedVpnUserIds;
    }

    @Override
    public String getUserName(VpnUser user){
        return VpnUtils.getUserName(user);
    }

    @Override
    public void deleteVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_DELETE.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }
        vpnUserSession.removeVpnUser(vpnUserId);

        // Audit logging
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPNUser deleted with id: " + vpnUserId);
        details.put("id", vpnUserId);
        securityEventsLoggerSession.log(EventTypes.VPN_USER_DELETE, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);
    }

    @Override
    public void revokeVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_REVOKE.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }
        vpnUserSession.revokeVpnUser(vpnUserId);

        // Audit logging
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPNUser revoked with id: " + vpnUserId);
        details.put("id", vpnUserId);
        securityEventsLoggerSession.log(EventTypes.VPN_USER_REVOKE, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);
    }

    @Override
    public VpnUser getVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_VIEW.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }
        return vpnUserSession.getVpnUser(vpnUserId);
    }

    public VpnUser downloadOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, Properties properties)
            throws AuthorizationDeniedException {
        // TODO: auth
        final VpnUser user = vpnUserSession.downloadOtp(vpnUserId, otpToken);
        if (user != null){
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", "VPN config OTP downloaded for usrId: " + vpnUserId);
            details.put("otpToken", otpToken);
            details.put("ip", properties.getProperty("ip"));
            details.put("fwded", properties.getProperty("fwded"));
            details.put("UA", properties.getProperty("ua"));
            securityEventsLoggerSession.log(EventTypes.VPN_OTP_DOWNLOADED, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                    authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);
        }

        return user;
    }

    @Override
    public VpnUser createVpnUser(final AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException
    {
        if (log.isTraceEnabled()) {
            log.trace(">createVpnUser: " + user.getEmail());
        }

        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_NEW.resource())) {
            throw new AuthorizationDeniedException();
        }

        final Set<Integer> allVpnUsers = new HashSet<>(vpnUserSession.getVpnUserIds());

        // Allocate new user ID
        Integer vpnUserId = null;
        for (int i = 0; i < 100; i++) {
            final int current = rnd.nextInt();
            if (!allVpnUsers.contains(current)) {
                vpnUserId = current;
                break;
            }
        }
        if (vpnUserId == null) {
            throw new RuntimeException("Failed to allocate a new vpnUserId.");
        }

        user.setId(vpnUserId);
        user = vpnUserSession.mergeVpnUser(user);

        // Audit logging
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPNUser created with id: " + vpnUserId);
        details.put("id", vpnUserId);
        details.put("email", user.getEmail());
        details.put("device", user.getDevice());
        securityEventsLoggerSession.log(EventTypes.VPN_USER_CREATE, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);

        if (log.isTraceEnabled()) {
            log.trace("<createVpnUser: " + user.getEmail());
        }

        return user;
    }

    /**
     * Asserts if an authentication token is authorized to modify vpn users
     *
     * @param authenticationToken the authentication token to check
     * @throws AuthorizationDeniedException thrown if authorization was denied.
     */
    private void assertAuthorizedToModifyVpnUsers(AuthenticationToken authenticationToken) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken, VpnRules.USER_MODIFY.resource())) {
            final String msg = INTRES.getLocalizedMessage("authorization.notuathorizedtoresource", VpnRules.USER_MODIFY.resource(),
                    authenticationToken.toString());
            throw new AuthorizationDeniedException(msg);
        }
    }

    @Override
    public VpnUser saveVpnUser(AuthenticationToken authenticationToken, VpnUser user)
            throws AuthorizationDeniedException, VpnUserNameInUseException {
        if (log.isTraceEnabled()) {
            log.trace(">saveVpnUser: " + user.getEmail());
        }

        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_REVOKE.resource() + "/" + user.getId())) {
            throw new AuthorizationDeniedException();
        }

        final VpnUser currentVpnUser = vpnUserSession.getVpnUser(user.getEmail(), user.getDevice());
        if(currentVpnUser == null){
            throw new NullPointerException(String.format("User %s does not exist", user.getEmail()));
        }

        // TODO: Merge somehow...
        // e.g., do not overwrite keys
        user = vpnUserSession.mergeVpnUser(user);

        // Audit logging
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPNUser changed with id: " + user.getId());
        details.put("id", user.getId());
        details.put("email", user.getEmail());
        details.put("device", user.getDevice());
        securityEventsLoggerSession.log(EventTypes.VPN_USER_CHANGE, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(user.getId()), null, null, details);

        if (log.isTraceEnabled()) {
            log.trace("<saveVpnUser: " + user.getEmail());
        }

        return user;
    }

    /**
     * Generates VPN configuration.
     *
     * @param authenticationToken
     * @param endEntity
     * @param ks
     * @return
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     */
    @Override
    public String generateVpnConfig(AuthenticationToken authenticationToken, EndEntityInformation endEntity, KeyStore ks)
            throws AuthorizationDeniedException, CADoesntExistsException {

        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.USER_VIEW.resource())) {
                throw new AuthorizationDeniedException();
            }
            // TODO: logging

            final CA ca = caSession.getCA(authenticationToken, endEntity.getCAId());
            final java.security.cert.Certificate caCert = ca.getCACertificate();
            final String caCertDN = CertTools.getSubjectDN(caCert);
            final String hostname = CertTools.getPartFromDN(caCertDN, "CN");
            final String caCertPem = VpnUtils.certificateToPem(caCert);

            final Certificate cert = ks.getCertificate(endEntity.getUsername());
            if (cert == null){
                log.error("Certificate is null, cannot generate config");
                return null;
            }

            final String certPem = VpnUtils.certificateToPem(cert);
            final Key key = ks.getKey(endEntity.getUsername(), null);
            final String keyPem = VpnUtils.privateKeyToPem((PrivateKey) key);

            final TemplateEngine templateEngine = LanguageHelper.getTemplateEngine();
            final Context ctx = new Context();
            ctx.setVariable("vpnhost", hostname);
            ctx.setVariable("entity", endEntity);
            ctx.setVariable("vpn_ca", caCertPem);
            ctx.setVariable("vpn_cert", certPem);
            ctx.setVariable("vpn_key", keyPem);

            final String tpl = templateEngine.process(VpnCons.VPN_CONFIG_TEMPLATE, ctx);
            return tpl;

        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding in VPN config generation", e);
        } catch (KeyStoreException e) {
            log.error("KeyStore exception in VPN config generation", e);
        } catch (UnrecoverableKeyException e) {
            log.error("Unrecoverable key exception in VPN config generation", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("NoSuchAlgorithmException in VPN config generation", e);
        } catch (IOException e) {
            log.error("IOException in VPN config generation", e);
        }

        return null;
    }
}
