package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.AuthenticationFailedException;
import org.cesecore.authentication.tokens.AuthenticationProvider;
import org.cesecore.authentication.tokens.AuthenticationSubject;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSession;
import org.cesecore.certificates.util.DNFieldExtractor;
import org.cesecore.util.CertTools;
import org.ejbca.core.ejb.ra.EndEntityManagementSession;
import org.ejbca.core.model.authorization.AccessRulesConstants;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * Utils and helper for web.
 * Created by dusanklinec on 23.02.17.
 */
public class VpnWebUtils {

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

        private AuthenticationProvider authenticationSession;
        private EndEntityManagementSession endEntityManagementSession;
        private AccessControlSession authorizationSession;

        public AdminAuthorization() {
        }

        public AdminAuthorization(AuthenticationProvider authenticationSession, EndEntityManagementSession endEntityManagementSession, AccessControlSession authorizationSession) {
            this.authenticationSession = authenticationSession;
            this.endEntityManagementSession = endEntityManagementSession;
            this.authorizationSession = authorizationSession;
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
         * Check authorization to the resource based on the client certificate in the request.
         * @param request request to extract user identity from
         * @param resources resources to test, can be null
         * @throws AuthenticationFailedException
         * @throws AuthorizationDeniedException
         */
        @SuppressWarnings("Duplicates")
        public void isAuthorized(HttpServletRequest request, String ... resources) throws AuthenticationFailedException, AuthorizationDeniedException {
            certificates = getClientCertificates(request);
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
    }
}
