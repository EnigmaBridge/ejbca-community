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
import org.bouncycastle.operator.OperatorCreationException;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.certificates.ca.*;
import org.cesecore.certificates.certificate.CertificateCreateException;
import org.cesecore.certificates.certificate.CertificateRevokeException;
import org.cesecore.certificates.certificate.IllegalKeyException;
import org.cesecore.certificates.certificate.exception.CertificateSerialNumberException;
import org.cesecore.certificates.certificate.exception.CustomCertificateSerialNumberException;
import org.cesecore.certificates.crl.CrlStoreSessionLocal;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSessionLocal;
import org.ejbca.core.ejb.ca.sign.SignSessionLocal;
import org.ejbca.core.ejb.crl.PublishingCrlSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.util.mail.MailSender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.ejb.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Management session bean for VPN functionality. Top level.
 *
 * @author ph4r05
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "VpnUserManagementRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class VpnUserManagementSessionBean implements VpnUserManagementSessionLocal, VpnUserManagementSessionRemote {

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
    private OtpDownloadSession otpDownloadSession;
    @EJB
    private CaSessionLocal caSession;
    @EJB
    private EndEntityManagementSessionLocal endEntityManagementSession;
    @EJB
    private EndEntityAuthenticationSessionLocal endEntityAuthenticationSession;
    @EJB
    private EndEntityAccessSessionLocal endEntityAccessSession;
    @EJB
    private SignSessionLocal signSession;
    @EJB
    private CrlStoreSessionLocal crlStoreSession;
    @EJB
    private PublishingCrlSessionLocal publishingCrlSession;

    @Override
    public List<Integer> geVpnUsersIds(AuthenticationToken authenticationToken) {
        final List<Integer> allVpnUsersIds = vpnUserSession.getVpnUserIds();
        final List<Integer> authorizedVpnUserIds = new ArrayList<Integer>();
        for (final Integer current : allVpnUsersIds) {
            authorizedVpnUserIds.add(current);
        }
        return authorizedVpnUserIds;
    }

    @Override
    public boolean isUsernameAvailable(AuthenticationToken authenticationToken, VpnUser user) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER.resource())) {
            throw new AuthorizationDeniedException();
        }

        return !vpnUserSession.isVpnUserNameUsed(user.getEmail(), user.getDevice());
    }

    @Override
    public String getUserName(VpnUser user){
        return VpnUtils.getUserName(user);
    }

    @Override
    public String getAdminRole(String cname){
        if (cname == null || cname.isEmpty()){
            return null;
        }

        final String[] parts = cname.split("/", 2);
        if (parts.length != 2){
            log.info("User format invalid: " + cname);
            return null;
        }

        try{
            final VpnUser vpnUser = vpnUserSession.getVpnUser(parts[0], parts[1]);
            if (vpnUser == null){
                return null;
            }

            return vpnUser.getAdminRole();

        } catch (Exception e){
            log.info("Exception in loading the user " + cname, e);
        }

        return null;
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
    public List<VpnUser> getVpnUsers(AuthenticationToken authenticationToken, List<Integer> vpnUserIds) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_VIEW.resource())) {
            throw new AuthorizationDeniedException();
        }

        return vpnUserSession.getVpnUsers(vpnUserIds);
    }

    @Override
    public VpnUser getVpnUser(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_VIEW.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }
        return vpnUserSession.getVpnUser(vpnUserId);
    }

    @Override
    public VpnUser getVpnUser(AuthenticationToken authenticationToken, String email, String device) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken, VpnRules.USER_VIEW.resource())) {
            throw new AuthorizationDeniedException();
        }
        return vpnUserSession.getVpnUser(email, device);
    }

    @Override
    public VpnUser checkOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, Properties properties) throws VpnOtpInvalidException, VpnOtpTooManyException, VpnOtpOldException, VpnNoConfigException, VpnOtpDescriptorException {
        final VpnUser user = vpnUserSession.downloadOtp(vpnUserId, otpToken);
        if (user == null || otpToken == null || !otpToken.equals(user.getOtpDownload())) {
            throw new VpnOtpInvalidException();
        }

        if (properties == null) {
            properties = new Properties();
        }

        // Build download spec.
        properties.remove(VpnCons.KEY_METHOD);
        final JSONObject specJson = VpnUtils.properties2json(properties);

        // Checking basic OTP validity conditions.
        checkOtpConditions(user, false);

        // Check descriptors.
        checkOtpDescriptor(user, specJson, false, true);

        // Copy, detach from the persistence context, reset sensitive fields.
        final VpnUser userCopy = VpnUser.copy(user);
        userCopy.setKeyStore(null);
        userCopy.setKeyStoreRaw(null);
        userCopy.setVpnConfig(null);

        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPN OTP check: " + vpnUserId);
        details.put("otpToken", otpToken);
        details.put(VpnCons.KEY_IP, properties.getProperty(VpnCons.KEY_IP));
        details.put(VpnCons.KEY_FORWARDED, properties.getProperty(VpnCons.KEY_FORWARDED));
        details.put(VpnCons.KEY_USER_AGENT, properties.getProperty(VpnCons.KEY_USER_AGENT));
        
        securityEventsLoggerSession.log(EventTypes.VPN_OTP_CHECK, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);

        return userCopy;
    }

    public VpnUser newNonceOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, String cookie, Properties properties) throws VpnOtpInvalidException, VpnOtpTooManyException, VpnOtpOldException, VpnOtpDescriptorException, VpnOtpCookieException {
        if (properties == null) {
            properties = new Properties();
        }

        // Analyse request method. If HEAD, cookie is not mandatory.
        final String requestMethod = properties.getProperty(VpnCons.KEY_METHOD);
        properties.remove(VpnCons.KEY_METHOD);

        final VpnUser user = vpnUserSession.downloadOtp(vpnUserId, otpToken);
        if (user == null || otpToken == null || !otpToken.equals(user.getOtpDownload())) {
            throw new VpnOtpInvalidException();
        }

        // Head preflight request - do nothing. Would generate redundant nonce.
        if (requestMethod.equalsIgnoreCase("head")){
            return user;
        }

        // Checking basic OTP validity conditions.
        checkOtpConditions(user, false);

        // Check descriptors.
        final JSONObject specJson = VpnUtils.properties2json(properties);
        checkOtpDescriptor(user, specJson, false, false);

        // Cookie already set - no more nonces
        final String otpCookie = user.getOtpCookie();
        if (otpCookie != null){
            throw new VpnOtpCookieException();
        }

        // otp nonce record.
        final String nonceRec = user.getOtpNonce();
        final JSONObject nonceJson = new JSONObject();
        nonceJson.put(VpnCons.OTP_NONCE_CNT, 1);
        nonceJson.put(VpnCons.OTP_NONCE, VpnUtils.genRandomPwd(8));
        nonceJson.put(VpnCons.OTP_NONCE_TIME, System.currentTimeMillis());

        if (nonceRec != null){
            final JSONObject prevNonceRec = new JSONObject(nonceRec);
            nonceJson.put(VpnCons.OTP_NONCE_CNT, 1 + prevNonceRec.getInt(VpnCons.OTP_NONCE_CNT));
        }

        user.setOtpNonce(nonceJson.toString());
        tryMergeUser(user);

        // Copy, detach from the persistence context
        final VpnUser userCopy = VpnUser.copy(user);

        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPN config OTP nonce for usrId: " + vpnUserId);
        details.put("otpToken", otpToken);
        details.put(VpnCons.KEY_IP, properties.getProperty(VpnCons.KEY_IP));
        details.put(VpnCons.KEY_FORWARDED, properties.getProperty(VpnCons.KEY_FORWARDED));
        details.put(VpnCons.KEY_USER_AGENT, properties.getProperty(VpnCons.KEY_USER_AGENT));

        if (cookie != null) {
            details.put("cookie", cookie);
        }
        securityEventsLoggerSession.log(EventTypes.VPN_OTP_NONCE, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);

        return userCopy;
    }

    @Override
    public VpnUser downloadOtp(AuthenticationToken authenticationToken, int vpnUserId, String otpToken, String cookie, Properties properties)
            throws AuthorizationDeniedException, VpnOtpOldException, VpnOtpTooManyException, VpnOtpCookieException, VpnOtpDescriptorException, VpnOtpInvalidException, VpnNoConfigException {

        if (properties == null) {
            properties = new Properties();
        }

        // Analyse request method. If HEAD, cookie is not mandatory.
        final String requestMethod = properties.getProperty(VpnCons.KEY_METHOD);
        properties.remove(VpnCons.KEY_METHOD);

        // Build download spec.
        final JSONObject specJson = VpnUtils.properties2json(properties);
        final String downloadSpec = specJson.toString();

        final VpnUser user = vpnUserSession.downloadOtp(vpnUserId, otpToken);
        if (user == null || otpToken == null || !otpToken.equals(user.getOtpDownload())) {
            throw new VpnOtpInvalidException();
        }

        // Checking basic OTP validity conditions.
        final long timeNow = System.currentTimeMillis();
        checkOtpConditions(user, true);

        // Check descriptors.
        checkOtpDescriptor(user, specJson, true, true);

        // If cookie is set in the database, require the same cookie.
        // Chrome on iOS does two concurrent requests. The one with HEAD does
        // not have cookie set correctly.
        // In that case we relax this condition - no cookie check.
        final boolean isHead = requestMethod.equalsIgnoreCase("head");
        final String otpCookie = user.getOtpCookie();
        if (otpCookie != null && !isHead && !otpCookie.equals(cookie)){
            clearOtp(user);
            tryMergeUser(user);
            throw new VpnOtpCookieException();
        }

        // Check there is config to download
        final String vpnConfig = user.getVpnConfig();
        if (vpnConfig == null || vpnConfig.isEmpty()){
            throw new VpnNoConfigException();
        }

        // Vpn seems valid. Update fields.
        user.setDateModified(timeNow);
        user.setOtpUsed(timeNow);
        user.setOtpUsedCount(user.getOtpUsedCount()+1);
        user.setOtpUsedDescriptor(downloadSpec);

        // Generate cookie on the first GET request.
        // Cookie rotation is disabled due to fishy behavior of the mobile browsers.
        if (user.getOtpCookie() == null) {
            user.setOtpCookie(VpnUtils.genRandomPwd());
        }

        tryMergeUser(user);

        // Copy, detach from the persistence context
        final VpnUser userCopy = VpnUser.copy(user);

        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPN config OTP downloaded for usrId: " + vpnUserId);
        details.put("otpToken", otpToken);
        details.put(VpnCons.KEY_IP, properties.getProperty(VpnCons.KEY_IP));
        details.put(VpnCons.KEY_FORWARDED, properties.getProperty(VpnCons.KEY_FORWARDED));
        details.put(VpnCons.KEY_USER_AGENT, properties.getProperty(VpnCons.KEY_USER_AGENT));

        if (cookie != null) {
            details.put("cookie", cookie);
        }
        securityEventsLoggerSession.log(EventTypes.VPN_OTP_DOWNLOADED, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);

        return userCopy;
    }

    /**
     * Checks OTP validity and used count.
     * In case OTP token is invalid anymore it is cleared from the database.
     *
     * @param user vpn user to check OTP validity for,
     * @param saveFirstUsed if true the time is saved on the first use
     * @throws VpnOtpOldException OTP token is too old
     * @throws VpnOtpTooManyException OTP used too many times
     */
    private void checkOtpConditions(VpnUser user, boolean saveFirstUsed) throws VpnOtpOldException, VpnOtpTooManyException {
        // Multiple times download is possible in several cases.
        // If OTP was used already check if it was not too long time ago.
        final long timeNow = System.currentTimeMillis();
        final Long otpFirstUsed = user.getOtpFirstUsed();
        if (otpFirstUsed != null && otpFirstUsed > 0) {
            if ((timeNow - otpFirstUsed) > 3L * 60L * 1000L) {
                clearOtp(user);
                tryMergeUser(user);
                throw new VpnOtpOldException();
            }
        } else if (saveFirstUsed) {
            // First OTP download.
            user.setOtpFirstUsed(timeNow);
        }

        // Check if the OTP was not used too many times. Max 5.
        final int otpUsedCount = user.getOtpUsedCount();
        if (otpUsedCount >= 4){
            clearOtp(user);
            tryMergeUser(user);
            throw new VpnOtpTooManyException();
        }
    }

    /**
     * Checks OTP descriptor
     * @param user
     * @param specJson
     * @throws VpnOtpDescriptorException
     */
    private void checkOtpDescriptor(VpnUser user, JSONObject specJson, boolean clearOnFail, boolean checkNonce) throws VpnOtpDescriptorException {
        final String otpUsedDescriptor = user.getOtpUsedDescriptor();
        if (otpUsedDescriptor != null) {
            final JSONObject dbDescriptor = new JSONObject(otpUsedDescriptor);
            if (!dbDescriptor.similar(specJson)){
                if (clearOnFail) {
                    clearOtp(user);
                    tryMergeUser(user);
                }
                throw new VpnOtpDescriptorException();
            }
        }
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

        // Admin role setup
        setAdminRoleForNewUserInternal(user);

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

    /**
     * Clears OTP related data
     * @param user vpn user to clear OTP data.
     */
    private void clearOtp(VpnUser user){
        user.setOtpDownload(null);
        user.setVpnConfig(null);
        user.setDateModified(System.currentTimeMillis());
    }

    /**
     * Tries to merge VPN user. Runtime exception is thrown in case of an error.
     * @param user vpn user to merge
     */
    private void tryMergeUser(VpnUser user){
        try {
            vpnUserSession.mergeVpnUser(user);
        } catch (VpnUserNameInUseException e) {
            throw new RuntimeException("VpnUser update failed", e);
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

    @Override
    public String getDefaultAdminRole(String email) {
        return getDefaultAdminRoleInternal(email);
    }

    @Override
    public void setAdminRoleForNewUser(AuthenticationToken authenticationToken, VpnUser vpnUser) {
        setAdminRoleForNewUserInternal(vpnUser);
    }

    /**
     * Send email with configuration.
     */
    @Override
    public void sendConfigurationEmail(AuthenticationToken authenticationToken, int vpnUserId, Properties properties)
            throws AuthorizationDeniedException, VpnMailSendException, IOException {

        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_MAIL.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }

        final VpnUser user = vpnUserSession.getVpnUser(vpnUserId);
        final String userName = VpnUtils.getUserName(user);
        final EndEntityInformation endEntity = endEntityAccessSession.findUser(authenticationToken, userName);

        if (user.getOtpDownload() == null){
            throw new VpnMailSendException("OTP is null");
        }

        final String userLang = user.getUsrLang();
        final ResourceBundle langBundle = LanguageHelper.loadLanguageResource(userLang);
        final TemplateEngine templateEngine = LanguageHelper.getTemplateEngine(userLang);
        final String receiverAddress = user.getEmail();
        final String senderAddress = VpnConfig.getEmailFromAddress();

        String hostname = null;
        String downloadLink = null;
        try {
            downloadLink = genConfigDownloadLink(authenticationToken, endEntity, user);
            final CA ca = caSession.getCA(authenticationToken, endEntity.getCAId());
            final java.security.cert.Certificate caCert = ca.getCACertificate();
            hostname = VpnUtils.extractCN(caCert);

        } catch(CADoesntExistsException e){
            throw new VpnMailSendException("Cannot generate the link", e);
        }

        final Context ctx = new Context();
        final Locale userLocale = LanguageHelper.getLocale(userLang);
        ctx.setLocale(userLocale == null ? LanguageHelper.getLocale(VpnConfig.getDefaultLanguage()) : userLocale);
        ctx.setVariable("user", user);
        ctx.setVariable("entity", endEntity);
        ctx.setVariable("vpn_link", downloadLink);
        ctx.setVariable("vpn_hostname", hostname);
        ctx.setVariable("generated_time", new Date());

        final String messageBody = templateEngine.process(VpnCons.VPN_EMAIL_TEMPLATE, ctx);
        try {
            MailSender.sendMailOrThrow(senderAddress,
                    Collections.singletonList(receiverAddress),
                    MailSender.NO_CC,
                    langBundle.getString("vpn.email.config.subject"),
                    messageBody,
                    MailSender.NO_ATTACHMENTS);

            final String logmsg = INTRES.getLocalizedMessage("vpn.email.config.sent", receiverAddress);
            log.info(logmsg);

            // Update VpnUser record.
            user.setLastMailSent(System.currentTimeMillis());
            vpnUserSession.mergeVpnUser(user);

            // Audit logging
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", "VPNUser mail sent");
            details.put("id", vpnUserId);
            details.put("email", receiverAddress);
            details.put("device", user.getDevice());
            securityEventsLoggerSession.log(EventTypes.VPN_MAIL_SENT, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                    authenticationToken.toString(), String.valueOf(vpnUserId), null, null, details);

        } catch (Exception e) {
            String msg = INTRES.getLocalizedMessage("vpn.email.config.errorsend", receiverAddress);
            log.info(msg, e);

            throw new VpnMailSendException(e);
        }
    }

    @Override
    public String getConfigDownloadLink(AuthenticationToken authenticationToken, int vpnUserId) throws AuthorizationDeniedException, VpnException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.USER_LINK.resource() + "/" + vpnUserId)) {
            throw new AuthorizationDeniedException();
        }

        final VpnUser user = vpnUserSession.getVpnUser(vpnUserId);
        final String userName = VpnUtils.getUserName(user);
        final EndEntityInformation endEntity = endEntityAccessSession.findUser(authenticationToken, userName);
        if (user.getOtpDownload() == null){
            return null;
        }

        try {
            return genConfigDownloadLink(authenticationToken, endEntity, user);

        } catch(CADoesntExistsException e){
            throw new VpnException("Cannot generate the link", e);
        }
    }

    /**
     * Generates VPN download link for the user to download the configuration.
     *
     * @param authenticationToken
     * @param endEntity
     * @param user
     * @return
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     */
    private String genConfigDownloadLink(AuthenticationToken authenticationToken, EndEntityInformation endEntity, VpnUser user)
            throws AuthorizationDeniedException, CADoesntExistsException
    {
        if (user.getOtpDownload() == null){
            throw new IllegalStateException("OTP is empty, cannot generate the link");
        }

        final int port = VpnConfig.getPublicHttpsPort();
        final CA ca = caSession.getCA(authenticationToken, endEntity.getCAId());
        final java.security.cert.Certificate caCert = ca.getCACertificate();
        final String hostname = VpnUtils.extractCN(caCert);

        return String.format("https://%s:%d/key?id=%d&otp=%s",
                hostname, port, user.getId(), user.getOtpDownload());
    }

    /**
     * Creates a new key store with valid private key and certificate signed by the user's CA.
     *
     * @param authenticationToken
     * @param endEntity
     * @return
     * @throws CustomCertificateSerialNumberException
     * @throws AuthStatusException
     * @throws InvalidAlgorithmParameterException
     * @throws CertificateSerialNumberException
     * @throws CryptoTokenOfflineException
     * @throws CertificateRevokeException
     * @throws FinderException
     * @throws OperatorCreationException
     * @throws AuthLoginException
     * @throws IllegalKeyException
     * @throws IOException
     * @throws CertificateCreateException
     * @throws VpnGenerationException
     * @throws AuthorizationDeniedException
     * @throws InvalidAlgorithmException
     * @throws SignRequestSignatureException
     * @throws CADoesntExistsException
     * @throws IllegalNameException
     * @throws CertificateException
     * @throws IllegalValidityException
     * @throws CAOfflineException
     * @throws UserDoesntFullfillEndEntityProfile
     */
    private KeyStore createKeys(AuthenticationToken authenticationToken, EndEntityInformation endEntity, OptionalNull<String> password)
            throws CustomCertificateSerialNumberException, AuthStatusException, InvalidAlgorithmParameterException,
            CertificateSerialNumberException, CryptoTokenOfflineException, CertificateRevokeException,
            FinderException, OperatorCreationException, AuthLoginException, IllegalKeyException,
            IOException, CertificateCreateException, VpnGenerationException, AuthorizationDeniedException,
            InvalidAlgorithmException, SignRequestSignatureException, CADoesntExistsException, IllegalNameException,
            CertificateException, IllegalValidityException, CAOfflineException, UserDoesntFullfillEndEntityProfile
    {
        final int tokentype = endEntity.getTokenType();
        final boolean createJKS = (tokentype == SecConst.TOKEN_SOFT_JKS);
        final boolean createPEM = (tokentype == SecConst.TOKEN_SOFT_PEM);

        final VpnCertGenerator generator = new VpnCertGenerator();
        generator.setFetchRemoteSessions(false);
        generator.setAuthenticationToken(authenticationToken);
        generator.setCaSession(caSession);
        generator.setEndEntityAccessSession(endEntityAccessSession);
        generator.setEndEntityAuthenticationSession(endEntityAuthenticationSession);
        generator.setSignSession(signSession);
        generator.setPassword(password);

        final KeyStore ks = generator.generateClient(endEntity, createJKS, createPEM, false, null);

        // If all was OK, users status is set to GENERATED by the signsession when the user certificate is created.
        // If status is still NEW, FAILED or KEYRECOVER though, it means we should set it back to what it was before,
        // probably it had a request counter meaning that we should not reset the clear text password yet.
        final EndEntityInformation vo = endEntityAccessSession.findUser(authenticationToken, endEntity.getUsername());
        if ((vo.getStatus() == EndEntityConstants.STATUS_NEW)
                || (vo.getStatus() == EndEntityConstants.STATUS_FAILED)
                || (vo.getStatus() == EndEntityConstants.STATUS_KEYRECOVERY))
        {
            endEntityManagementSession.setClearTextPassword(authenticationToken, endEntity.getUsername(), endEntity.getPassword());
        } else {
            // Delete clear text password, if we are not letting status be the same as originally
            // We use autogenerated passwords here so this regenerates the new password and stores it
            // in the clear text field.
            endEntityManagementSession.setClearTextPassword(authenticationToken, endEntity.getUsername(), null);
        }

        String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.generateduser", endEntity.getUsername());
        log.info(iMsg);

        return ks;
    }

    /**
     * Generates new VPN creds.
     *
     * @param authenticationToken auth token
     * @param vpnUserId user entity ID
     * @param password optionally a password.
     * @param properties optional properties
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     * @throws IOException
     * @throws VpnException
     */
    @Override
    public VpnUser newVpnCredentials(AuthenticationToken authenticationToken, int vpnUserId, OptionalNull<String> password, Properties properties)
            throws AuthorizationDeniedException, CADoesntExistsException, IOException, VpnException {
        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.USER_GENERATE.resource() + "/" + vpnUserId)) {
                throw new AuthorizationDeniedException();
            }

            final VpnUser user = vpnUserSession.getVpnUser(vpnUserId);
            final String userName = VpnUtils.getUserName(user);
            final EndEntityInformation endEntity = endEntityAccessSession.findUser(authenticationToken, userName);

            final KeyStore ks = createKeys(authenticationToken, endEntity, password);
            VpnUtils.addKeyStoreToUser(user, ks, VpnConfig.getKeyStorePass().toCharArray());

            // Generate VPN configuration
            user.setOtpUsed(null);
            user.setOtpFirstUsed(null);
            user.setOtpCookie(null);
            user.setOtpUsedCount(0);
            user.setOtpUsedDescriptor(null);
            user.setOtpDownload(VpnUtils.genRandomPwd());
            user.setLastMailSent(null);
            user.setConfigGenerated(System.currentTimeMillis());

            final Integer prevVersion = user.getConfigVersion();
            user.setConfigVersion(prevVersion == null ? 1 : prevVersion + 1);

            final String vpnConfig = generateVpnConfigData(authenticationToken, endEntity, user, ks);
            user.setVpnConfig(vpnConfig);

            final VpnUser mergedUser = vpnUserSession.mergeVpnUser(user);
            final boolean returnKeyStore = properties != null && !properties.getProperty(VpnCons.KEY_RETURN_KEY_STORE).isEmpty();
            if (returnKeyStore){
                mergedUser.setKeyStoreRaw(ks);
            }

            // Audit logging
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", "VPNUser new credentials");
            details.put("id", user.getId());
            details.put("email", user.getEmail());
            details.put("device", user.getDevice());
            details.put("returnKeyStore", returnKeyStore);
            securityEventsLoggerSession.log(EventTypes.VPN_MAIL_SENT, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                    authenticationToken.toString(), String.valueOf(user.getId()), null, null, details);

            return mergedUser;
        } catch (AuthorizationDeniedException | CADoesntExistsException | IOException e){
            throw e;
        } catch(Exception e){
            throw new VpnException("Exception in creating new VPN credentials", e);
        }
    }

    public String generateVpnConfig(AuthenticationToken authenticationToken, VpnUser user, VpnGenOptions options)
            throws AuthorizationDeniedException, CADoesntExistsException {

        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.USER_VIEW.resource())) {
                throw new AuthorizationDeniedException();
            }

            final String vpnConfig = user.getVpnConfig();
            if (vpnConfig == null || vpnConfig.isEmpty()){
                return null;
            }

            if (!vpnConfig.startsWith("{")){
                return vpnConfig;
            }

            // Translate config JSON to the templating context
            final JSONObject json = new JSONObject(vpnConfig);
            final JSONObject config = json.getJSONObject(VpnCons.VPN_CFG);

            final TemplateEngine templateEngine = LanguageHelper.getTemplateEngine();
            final Context ctx = new Context();

            final Iterator<String> keys = config.keys();
            while(keys.hasNext()){
                final String curKey = keys.next();
                final Object obj = config.get(curKey);
                if (obj instanceof JSONObject || obj instanceof JSONArray){
                    continue;
                }

                ctx.setVariable(curKey, obj);
            }

            ctx.setVariable(VpnCons.VPN_CFG_USER, user);
            ctx.setVariable("generated_time", new Date(user.getConfigGenerated()));

            // Candidate template names
            LinkedList<String> candidateTemplates = new LinkedList<>();
            candidateTemplates.add(VpnCons.VPN_CONFIG_TEMPLATE);

            // OS - dependent template name, higher priority.
            OperatingSystem os = null;
            if (options != null && options.getOs() != null) {
                os = options.getOs().getGroup();
                final String osTemplateSuffix = VpnUtils.sanitizeFileName(os.getName().toLowerCase().trim());
                candidateTemplates.add(0, VpnCons.VPN_CONFIG_TEMPLATE + "_" + osTemplateSuffix);
            }

            // Try each template according to the preference.
            for(String templateName : candidateTemplates){
                try{
                    String tpl = templateEngine.process(templateName, ctx);
                    if (os != null && OperatingSystem.WINDOWS.equals(os)){
                        tpl = VpnUtils.toWindowsEOL(tpl);
                    }

                    return tpl;
                } catch(Exception e){

                }
            }

            log.warn("No suitable template file was found");
            return null;

        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding in VPN config generation", e);
        } catch (IOException e) {
            log.error("IOException in VPN config generation", e);
        }

        return null;
    }

    /**
     * Generates a structure with information needed to generate a new VPN configuration.
     *
     * @param authenticationToken auth token
     * @param user user end entity
     * @param ks key store
     * @return VPN configuration
     * @throws AuthorizationDeniedException token invalid
     * @throws CADoesntExistsException invalid CA in the end entity
     */
    private String generateVpnConfigData(AuthenticationToken authenticationToken, EndEntityInformation endEntity, VpnUser user, KeyStore ks)
            throws AuthorizationDeniedException, CADoesntExistsException {

        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.USER_VIEW.resource())) {
                throw new AuthorizationDeniedException();
            }

            final CA ca = caSession.getCA(authenticationToken, endEntity.getCAId());
            final java.security.cert.Certificate caCert = ca.getCACertificate();
            final String hostname = VpnUtils.extractCN(caCert);
            final String caCertPem = VpnUtils.certificateToPem(caCert).trim();

            final Certificate cert = ks.getCertificate(endEntity.getUsername());
            if (cert == null){
                log.error("Certificate is null, cannot generate config");
                return null;
            }

            final String certPem = VpnUtils.certificateToPem(cert).trim();
            final Key key = ks.getKey(endEntity.getUsername(), null);
            final String keyPem = VpnUtils.privateKeyToPem((PrivateKey) key).trim();

            final JSONObject json = new JSONObject();
            final JSONObject config = new JSONObject();
            json.put(VpnCons.VPN_CFG_VERSION, 1);
            json.put(VpnCons.VPN_CFG, config);

            config.put(VpnCons.VPN_CFG_HOSTNAME, hostname);
            config.put(VpnCons.VPN_CFG_ENTITY_USERNAME, endEntity.getUsername());
            config.put(VpnCons.VPN_CFG_CA, caCertPem);
            config.put(VpnCons.VPN_CFG_CERT, certPem);
            config.put(VpnCons.VPN_CFG_KEY, keyPem);
            return json.toString();

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

    @Override
    public Integer generateCRL(AuthenticationToken authenticationToken, boolean force, Long overlapMilli) throws AuthorizationDeniedException, CADoesntExistsException, VpnException {
        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.CRL_GEN.resource())) {
                throw new AuthorizationDeniedException();
            }

            boolean wasGenerated = false;
            final CAInfo vpnCA = caSession.getCAInfo(authenticationToken, VpnConfig.getCA());
            if (force){
                wasGenerated = publishingCrlSession.forceCRL(authenticationToken, vpnCA.getCAId());
            } else {
                final long overlapMilliArg = overlapMilli != null ? overlapMilli : VpnConfig.getDefaultCrlOverlapMilli();
                wasGenerated = publishingCrlSession.createCRLNewTransactionConditioned(
                        authenticationToken, vpnCA.getCAId(), overlapMilliArg);
            }

            // Get the newest CRL number.
            if (wasGenerated) {
                final int number = crlStoreSession.getLastCRLNumber(vpnCA.getSubjectDN(), false);
                log.info("CRL with number " + number + " generated.");
                return number;
            }

            return null;

        } catch (AuthorizationDeniedException | CADoesntExistsException e){
            throw e;
        } catch(Exception e){
            throw new VpnException("Exception in loading CRL", e);
        }
    }

    @Override
    public byte[] getCRL(AuthenticationToken authenticationToken) throws AuthorizationDeniedException, CADoesntExistsException, VpnException {
        try {
            if (!accessControlSessionSession.isAuthorized(authenticationToken,
                    VpnRules.CRL_GET.resource())) {
                throw new AuthorizationDeniedException();
            }

            final CAInfo vpnCA = caSession.getCAInfo(authenticationToken, VpnConfig.getCA());
            final byte[] crl = crlStoreSession.getLastCRL(vpnCA.getSubjectDN(), false);
            return crl;

        } catch (AuthorizationDeniedException | CADoesntExistsException e){
            throw e;
        } catch(Exception e){
            throw new VpnException("Exception in loading CRL", e);
        }
    }

    /**
     * Updates the admin role for user being created according to the admin roles policies in place.
     * If the admin role is same for all users based on their email this is going to update
     * admin role of the user according to the already existing records in the database.
     * @param vpnUser
     */
    private void setAdminRoleForNewUserInternal(VpnUser vpnUser){
        final String existingRole = vpnUser.getAdminRole();

        // Role was explicitly set to null by a special placeholder
        if (VpnCons.ROLE_NONE.equals(existingRole)){
            vpnUser.setAdminRole(null);
            return;
        }

        // Role was explicitly set to a different value
        if (existingRole != null){
            return;
        }

        // Also handles the case if there is no previous user -> set to null.
        vpnUser.setAdminRole(getDefaultAdminRoleInternal(vpnUser.getEmail()));
    }

    /**
     * Returns default admin role for the user with given email.
     * Returns null if there is no admin policy in place or no user is there.
     *
     * @param email
     * @return
     */
    private String getDefaultAdminRoleInternal(String email){
        if (!VpnConfig.isAdminRoleEmailBased()){
            return null;
        }

        // Get users with the same email from the database.
        final List<VpnUser> users = vpnUserSession.getVpnUser(email);
        String lastRole = null;
        for(VpnUser user : users){
            lastRole = user.getAdminRole();
        }

        return lastRole;
    }

    //
    // OtpDownload token section
    //   General purpose OTP download tokens.
    //   In future may be refactored to a separate session bean, OTP logic can be generalized from the VpnUser
    //   to a common interfaces.
    //

    public OtpDownload otpGet(AuthenticationToken authenticationToken, String otpType, String otpId, String otpResource) throws AuthorizationDeniedException {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.OTP_GET.resource())) {
            throw new AuthorizationDeniedException();
        }

        final List<OtpDownload> otps = otpDownloadSession.getOtp(otpType, otpId, otpResource);
        if (otps == null || otps.isEmpty()){
            return null;
        }

        return otps.get(0);
    }

    public OtpDownload otpNew(final AuthenticationToken authenticationToken, OtpDownload token)
            throws AuthorizationDeniedException, VpnUserNameInUseException
    {
        if (!accessControlSessionSession.isAuthorized(authenticationToken,
                VpnRules.OTP_NEW.resource())) {
            throw new AuthorizationDeniedException();
        }

        // Remove token if it exists already - unique triplet.
        otpDownloadSession.remove(token.getOtpType(), token.getOtpId(), token.getOtpResource());

        final Set<Integer> allDbIds = new HashSet<>(otpDownloadSession.getIds());

        // Allocate new ID
        Integer otpDownloadId = null;
        for (int i = 0; i < 100; i++) {
            final int current = rnd.nextInt();
            if (!allDbIds.contains(current)) {
                otpDownloadId = current;
                break;
            }
        }
        if (otpDownloadId == null) {
            throw new RuntimeException("Failed to allocate a new otpDownloadId.");
        }

        final long curTime = System.currentTimeMillis();
        token.setId(otpDownloadId);
        token.setOtpUsedCount(0);
        token.setDateCreated(curTime);
        token.setDateModified(curTime);

        if (token.getOtpDownload() == null){
            token.setOtpDownload(VpnUtils.genRandomPwd());
        }

        token = otpDownloadSession.merge(token);

        // Audit logging
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "OTP created with id: " + otpDownloadId);
        details.put("id", otpDownloadId);
        details.put("otpType", token.getOtpType());
        details.put("otpId", token.getOtpId());
        details.put("otpResource", token.getOtpResource());
        securityEventsLoggerSession.log(EventTypes.VPN_OTP_NEW, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(otpDownloadId), null, null, details);
        return token;
    }

    @Override
    public OtpDownload otpCheckOtp(AuthenticationToken authenticationToken, String otpToken, Properties properties) throws VpnOtpInvalidException, VpnOtpTooManyException, VpnOtpOldException, VpnNoConfigException, VpnOtpDescriptorException {
        final OtpDownload token = otpDownloadSession.downloadOtp(otpToken);
        if (token == null || otpToken == null || !otpToken.equals(token.getOtpDownload())) {
            throw new VpnOtpInvalidException();
        }

        if (properties == null) {
            properties = new Properties();
        }

        // Build download spec.
        properties.remove(VpnCons.KEY_METHOD);
        final JSONObject specJson = VpnUtils.properties2json(properties);

        // Checking basic OTP validity conditions.
        checkOtpConditions(token, false);

        // Check descriptors.
        final String otpUsedDescriptor = token.getOtpUsedDescriptor();
        if (otpUsedDescriptor != null) {
            final JSONObject dbDescriptor = new JSONObject(otpUsedDescriptor);
            if (!dbDescriptor.similar(specJson)){
                throw new VpnOtpDescriptorException();
            }
        }

        // Copy, detach from the persistence context, reset sensitive fields.
        final OtpDownload tokenCopy = OtpDownload.copy(token);

        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPN OTP download check");
        details.put("otpToken", otpToken);
        details.put(VpnCons.KEY_IP, properties.getProperty(VpnCons.KEY_IP));
        details.put(VpnCons.KEY_FORWARDED, properties.getProperty(VpnCons.KEY_FORWARDED));
        details.put(VpnCons.KEY_USER_AGENT, properties.getProperty(VpnCons.KEY_USER_AGENT));

        securityEventsLoggerSession.log(EventTypes.VPN_OTP_OTP_CHECK, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(otpToken), null, null, details);

        return tokenCopy;
    }

    @Override
    public OtpDownload otpDownloadOtp(AuthenticationToken authenticationToken, String otpToken, String cookie, Properties properties)
            throws AuthorizationDeniedException, VpnOtpOldException, VpnOtpTooManyException, VpnOtpCookieException, VpnOtpDescriptorException, VpnOtpInvalidException {

        if (properties == null) {
            properties = new Properties();
        }

        // Analyse request method. If HEAD, cookie is not mandatory.
        final String requestMethod = properties.getProperty(VpnCons.KEY_METHOD);
        properties.remove(VpnCons.KEY_METHOD);

        // Build download spec.
        final JSONObject specJson = VpnUtils.properties2json(properties);
        final String downloadSpec = specJson.toString();

        final OtpDownload token = otpDownloadSession.downloadOtp(otpToken);
        if (token == null || otpToken == null || !otpToken.equals(token.getOtpDownload())) {
            throw new VpnOtpInvalidException();
        }

        // Checking basic OTP validity conditions.
        final long timeNow = System.currentTimeMillis();
        checkOtpConditions(token, true);

        // Check descriptors.
        final String otpUsedDescriptor = token.getOtpUsedDescriptor();
        if (otpUsedDescriptor != null) {
            final JSONObject dbDescriptor = new JSONObject(otpUsedDescriptor);
            if (!dbDescriptor.similar(specJson)){
                clearOtp(token);
                tryMergeOtp(token);
                throw new VpnOtpDescriptorException();
            }
        }

        // If cookie is set in the database, require the same cookie.
        // Chrome on iOS does two concurrent requests. The one with HEAD does
        // not have cookie set correctly.
        // In that case we relax this condition - no cookie check.
        final boolean isHead = requestMethod.equalsIgnoreCase("head");
        final String otpCookie = token.getOtpCookie();
        if (otpCookie != null && !isHead && !otpCookie.equals(cookie)){
            clearOtp(token);
            tryMergeOtp(token);
            throw new VpnOtpCookieException();
        }

        // Vpn seems valid. Update fields.
        token.setDateModified(timeNow);
        token.setOtpUsed(timeNow);
        token.setOtpUsedCount(token.getOtpUsedCount()+1);
        token.setOtpUsedDescriptor(downloadSpec);

        // Generate cookie on the first GET request.
        // Cookie rotation is disabled due to fishy behavior of the mobile browsers.
        if (token.getOtpCookie() == null) {
            token.setOtpCookie(VpnUtils.genRandomPwd());
        }

        tryMergeOtp(token);

        // Copy, detach from the persistence context
        final OtpDownload tokenCopy = OtpDownload.copy(token);

        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", "VPN OTP downloaded for token: " + tokenCopy.getOtpId());
        details.put("otpToken", otpToken);
        details.put(VpnCons.KEY_IP, properties.getProperty(VpnCons.KEY_IP));
        details.put(VpnCons.KEY_FORWARDED, properties.getProperty(VpnCons.KEY_FORWARDED));
        details.put(VpnCons.KEY_USER_AGENT, properties.getProperty(VpnCons.KEY_USER_AGENT));

        if (cookie != null) {
            details.put("cookie", cookie);
        }
        securityEventsLoggerSession.log(EventTypes.VPN_OTP_OTP_DOWNLOAD, EventStatus.SUCCESS, ModuleTypes.VPN, ServiceTypes.CORE,
                authenticationToken.toString(), String.valueOf(tokenCopy.getOtpId()), null, null, details);

        return tokenCopy;
    }

    /**
     * Checks OTP validity and used count.
     * In case OTP token is invalid anymore it is cleared from the database.
     *
     * @param token token validation
     * @param saveFirstUsed if true the time is saved on the first use
     * @throws VpnOtpOldException OTP token is too old
     * @throws VpnOtpTooManyException OTP used too many times
     */
    private void checkOtpConditions(OtpDownload token, boolean saveFirstUsed) throws VpnOtpOldException, VpnOtpTooManyException {
        // Multiple times download is possible in several cases.
        // If OTP was used already check if it was not too long time ago.
        final long timeNow = System.currentTimeMillis();
        final Long otpFirstUsed = token.getOtpFirstUsed();
        if (otpFirstUsed != null && otpFirstUsed > 0) {
            if ((timeNow - otpFirstUsed) > 3L * 60L * 1000L) {
                clearOtp(token);
                tryMergeOtp(token);
                throw new VpnOtpOldException();
            }
        } else if (saveFirstUsed) {
            // First OTP download.
            token.setOtpFirstUsed(timeNow);
        }

        // Check if the OTP was not used too many times. Max 5.
        final int otpUsedCount = token.getOtpUsedCount();
        if (otpUsedCount >= 4){
            clearOtp(token);
            tryMergeOtp(token);
            throw new VpnOtpTooManyException();
        }
    }

    /**
     * Tries to merge OtpDownload. Runtime exception is thrown in case of an error.
     * @param token OTP token to merge
     */
    private void tryMergeOtp(OtpDownload token){
        otpDownloadSession.merge(token);
    }

    /**
     * Clears OTP related data
     * @param token token to clear OTP data.
     */
    private void clearOtp(OtpDownload token){
        token.setOtpDownload(null);
        token.setDateModified(System.currentTimeMillis());
    }
}
