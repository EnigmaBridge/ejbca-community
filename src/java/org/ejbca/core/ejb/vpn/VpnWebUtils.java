package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.AuthenticationFailedException;
import org.cesecore.authentication.tokens.AuthenticationProvider;
import org.cesecore.authentication.tokens.AuthenticationSubject;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSession;
import org.cesecore.certificates.certificate.CertificateStoreSession;
import org.cesecore.certificates.util.DNFieldExtractor;
import org.cesecore.util.CertTools;
import org.ejbca.core.ejb.ra.EndEntityManagementSession;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utils and helper for web.
 * Created by dusanklinec on 23.02.17.
 */
public class VpnWebUtils {
    private static final Logger log = Logger.getLogger(VpnWebUtils.class);

    /**
     * Returns an array of TLS client certificates.
     * @param request request to check client certificates in
     * @return null, empty or an array
     */
    public static X509Certificate[] getClientCertificates(HttpServletRequest request) {
        return (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    }

    /**
     * Returns true if the request was made with the client certificate.
     * @param request
     * @return
     */
    public static boolean hasClientCertificate(HttpServletRequest request){
        final X509Certificate[] certificates = getClientCertificates(request);
        return certificates != null && certificates.length > 0;
    }

    /**
     * Method that returns the servername, extracted from the HTTPServlet Request, no protocol, port or application path is returned
     *
     * @return the server name requested
     */
    public static String getRequestServerName(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        // Remove https://
        requestURL = requestURL.substring(8);
        int firstSlash = requestURL.indexOf("/");
        // Remove application path
        requestURL = requestURL.substring(0, firstSlash);
        return requestURL;
    }

    /**
     * Simple GET request on the given URL with timeout
     * @param url url to send GET request
     * @return string response
     * @throws IOException
     */
    public static String getRequest(String url) throws IOException {
        final URL obj = new URL(url);
        final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "EJBCA");
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);

        final int responseCode = con.getResponseCode();
        if (responseCode/100 != 2){
            log.info("Auth server returned non-success code: " + responseCode);
            return null;
        }

        final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        final StringBuilder response = new StringBuilder();

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    /**
     * Queries local VPN auth server for currently connected user.
     * @param remoteAddr
     * @return
     */
    public static JSONObject queryVpnAuthServer(String remoteAddr){
        try{
            final String url = "http://127.0.0.1:32080/api/v1.0/verify?ip=" + remoteAddr;
            JSONObject res = null;

            for(int attempt=0; attempt<3; attempt++){
                final String response = getRequest(url);
                if (response == null){
                    continue;
                }

                res = new JSONObject(response);
            }

            boolean success = res.getBoolean("result");
            if (!success){
                return null;
            }

            return res.getJSONObject("user");

        } catch(Exception ex){
            log.warn("VPN auth exception", ex);
            return null;
        }
    }

    /**
     * Builds a new auth checker.
     * @param authenticationSession
     * @param endEntityManagementSession
     * @param authorizationSession
     * @return
     */
    public static AdminAuthorization buildAdminChecker(AuthenticationProvider authenticationSession, EndEntityManagementSession endEntityManagementSession, AccessControlSession authorizationSession){
        return new AdminAuthorization(authenticationSession, endEntityManagementSession, authorizationSession);
    }

    /**
     * Builds a new auth checker.
     * @param authenticationSession
     * @param endEntityManagementSession
     * @param authorizationSession
     * @return
     */
    public static AdminAuthorization buildAdminChecker(AuthenticationProvider authenticationSession,
                                                       EndEntityManagementSession endEntityManagementSession,
                                                       AccessControlSession authorizationSession,
                                                       VpnUserManagementSession vpnUserManagementSession,
                                                       CertificateStoreSession certStoreSession
                                                       ){
        return new AdminAuthorization(authenticationSession, endEntityManagementSession, authorizationSession,
                vpnUserManagementSession, certStoreSession);
    }

    /**
     * Admin auth checker.
     * Checks user authorization to the resource based on the client certificate in the request.
     */
    public static class AdminAuthorization {
        private static final Logger log = Logger.getLogger(AdminAuthorization.class);

        private X509Certificate[] certificates;
        private AuthenticationToken administrator;
        private String usercommonname;
        private String certificatefingerprint;
        private String remoteip;
        private String forwardedip;

        private String lastVpnAuthJson;
        private String lastVpnAuthCname;
        private String lastVpnAuthAdminRole;

        private AuthenticationProvider authenticationSession;
        private EndEntityManagementSession endEntityManagementSession;
        private AccessControlSession authorizationSession;
        private VpnUserManagementSession vpnUserManagementSession;
        private CertificateStoreSession certStoreSession;

        public AdminAuthorization() {
        }

        public AdminAuthorization(AuthenticationProvider authenticationSession,
                                  EndEntityManagementSession endEntityManagementSession,
                                  AccessControlSession authorizationSession) {
            this.authenticationSession = authenticationSession;
            this.endEntityManagementSession = endEntityManagementSession;
            this.authorizationSession = authorizationSession;
        }

        public AdminAuthorization(AuthenticationProvider authenticationSession,
                                  EndEntityManagementSession endEntityManagementSession,
                                  AccessControlSession authorizationSession,
                                  VpnUserManagementSession vpnUserManagementSession,
                                  CertificateStoreSession certStoreSession) {
            this.authenticationSession = authenticationSession;
            this.endEntityManagementSession = endEntityManagementSession;
            this.authorizationSession = authorizationSession;
            this.vpnUserManagementSession = vpnUserManagementSession;
            this.certStoreSession = certStoreSession;
        }

        /**
         * Returns true if the user is authorized for the resource
         * @param request request to extract user identity from
         * @return true if the user with client cert is an admin, false otherwise.
         */
        public boolean tryIsAdmin(HttpServletRequest request) {
            return tryIsAuthorized(request, AccessRulesConstants.ROLE_ADMINISTRATOR);
        }

        /**
         * Returns true if the user is authorized for the resource
         * @param request request to extract user identity from
         * @param resources resources to test, can be null
         * @return true if the user with client cert is authorized to the resource, false otherwise.
         */
        public boolean tryIsAuthorized(HttpServletRequest request, String ... resources) {
            try{
                isAuthorized(request, resources);
                return true;
            } catch(AuthenticationFailedException | AuthorizationDeniedException ignored){

            }

            return false;
        }

        /**
         * Returns true if VPN auth works for this user.
         * @param request
         * @return
         */
        public boolean tryIsAuthorizedVpn(HttpServletRequest request) {
            try{
                isAuthorizedVpn(request);
                return true;
            } catch(Exception e) {
                log.error("Error in VPN auth", e);
            }

            return false;
        }

        /**
         * Returns authorization status based on the VPN connection status.
         * @param request
         */
        public void isAuthorizedVpn(HttpServletRequest request) throws AuthenticationFailedException {
            if (vpnUserManagementSession == null || certStoreSession == null){
                throw new AuthenticationFailedException("System does not accept VPN auth");
            }

            // Get remote IP.
            // Try to auth only if connected via VPN.
            final String remoteAddr = request.getRemoteAddr();
            try {
                if (!VpnUtils.isIpInVPNNetwork(remoteAddr)){
                    throw new AuthenticationFailedException("Not connected via VPN");
                }
            } catch (UnknownHostException e) {
                throw new AuthenticationFailedException("VPN auth failed");
            }

            // Query VPN auth server for the user state, CNAME, etc...
            final JSONObject json = queryVpnAuthServer(remoteAddr);
            if (json == null){
                throw new AuthenticationFailedException("User could not be authenticated against VPN auth server.");
            }

            try {
                lastVpnAuthJson = json.toString();
                log.info("VPNAuth: " + lastVpnAuthJson);

                final String cname = json.getString("cname");
                lastVpnAuthCname = cname;

                final String adminRole = vpnUserManagementSession.getAdminRole(cname);
                lastVpnAuthAdminRole = adminRole;

                if (adminRole == null){
                    throw new AuthenticationFailedException("User is not permitted to act as an admin");
                }

                // load certificate from cert store...
                loadCertificatesForAdminRole(adminRole);

            } catch(Exception e){
                log.error("Error in VPN auth", e);
                throw new AuthenticationFailedException("User could not be authenticated against VPN auth server.");
            }

        }

        /**
         * Loads certificate as a cert chain for the given admin role
         * @param adminRole admin user to load certs for
         * @throws AuthenticationFailedException
         */
        public void loadCertificatesForAdminRole(String adminRole) throws AuthenticationFailedException {
            final List<Certificate> certs = certStoreSession.findCertificatesByUsername(adminRole);
            if (certs == null || certs.isEmpty()){
                throw new AuthenticationFailedException("User was not found");
            }

            certificates = new X509Certificate[] { (X509Certificate) certs.get(0)};
        }

        /**
         * Check authorization to the resource based on the client certificate in the request.
         * @param request request to extract user identity from
         * @param resources resources to test, can be null
         * @throws AuthenticationFailedException
         * @throws AuthorizationDeniedException
         */
        @SuppressWarnings("Duplicates")
        public void isAuthorized(HttpServletRequest request, String ... resources) throws AuthenticationFailedException, AuthorizationDeniedException {
            certificates = getClientCertificates(request);
            if (certificates == null || certificates.length == 0){
                try {
                    isAuthorizedVpn(request);
                } catch(Exception e){
                    throw new AuthenticationFailedException("Client certificate required.");
                }
            }

            if (certificates == null || certificates.length == 0) {
                throw new AuthenticationFailedException("Client certificate required.");

            } else {
                final Set<X509Certificate> credentials = new HashSet<X509Certificate>();
                credentials.add(certificates[0]);
                final AuthenticationSubject subject = new AuthenticationSubject(null, credentials);
                administrator = authenticationSession.authenticate(subject);
                if (administrator == null) {
                    throw new AuthenticationFailedException("Authentication failed for certificate: "+ CertTools.getSubjectDN(certificates[0]));
                }
            }

            // Set ServletContext for reading language files from resources
            // Check if certificate and user is an RA Admin
            final String userdn = CertTools.getSubjectDN(certificates[0]);
            final DNFieldExtractor dn = new DNFieldExtractor(userdn, DNFieldExtractor.TYPE_SUBJECTDN);
            usercommonname = dn.getField(DNFieldExtractor.CN, 0);

            final String issuerDN = CertTools.getIssuerDN(certificates[0]);
            final BigInteger serno = CertTools.getSerialNumber(certificates[0]);
            certificatefingerprint = CertTools.getFingerprintAsString(certificates[0]);
            if(!endEntityManagementSession.checkIfCertificateBelongToUser(serno, issuerDN)) {
                throw new AuthenticationFailedException("Certificate with SN " +  serno + " did not belong to user " + issuerDN);
            }

            remoteip = request.getRemoteAddr();

            String addr = request.getHeader("X-Forwarded-For");
            if (addr != null) {
                addr = addr.replaceAll("[^a-zA-Z0-9.:-_]", "?");
            }

            forwardedip = addr;

            if (resources.length>0 && !authorizationSession.isAuthorized(administrator, resources)) {
                throw new AuthorizationDeniedException("You are not authorized to view this page.");
            }
        }

        public X509Certificate[] getCertificates() {
            return certificates;
        }

        public AuthenticationToken getAdministrator() {
            return administrator;
        }

        public String getUsercommonname() {
            return usercommonname;
        }

        public String getCertificatefingerprint() {
            return certificatefingerprint;
        }

        public String getRemoteip() {
            return remoteip;
        }

        public String getForwardedip() {
            return forwardedip;
        }

        public String getLastVpnAuthJson() {
            return lastVpnAuthJson;
        }

        public String getLastVpnAuthCname() {
            return lastVpnAuthCname;
        }

        public String getLastVpnAuthAdminRole() {
            return lastVpnAuthAdminRole;
        }
    }
}
