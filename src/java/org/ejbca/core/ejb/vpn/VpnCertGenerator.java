package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.*;
import org.cesecore.certificates.certificate.CertificateCreateException;
import org.cesecore.certificates.certificate.CertificateRevokeException;
import org.cesecore.certificates.certificate.IllegalKeyException;
import org.cesecore.certificates.certificate.exception.CertificateSerialNumberException;
import org.cesecore.certificates.certificate.exception.CustomCertificateSerialNumberException;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.CertTools;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSession;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSessionRemote;
import org.ejbca.core.ejb.ca.sign.SignSession;
import org.ejbca.core.ejb.ca.sign.SignSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityAccessSession;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;

import javax.ejb.ObjectNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

/**
 * Generator of the VPN certificates / keys.
 *
 * @author ph4r05
 * Created by dusanklinec on 12.01.17.
 */
public class VpnCertGenerator extends VpnBaseHelper {
    private static final Logger log = Logger.getLogger(VpnCertGenerator.class);

    private CaSession caSession;
    private EndEntityAccessSession endEntityAccessSession;
    private EndEntityAuthenticationSession endEntityAuthenticationSession;
    private SignSession signSession;

    /**
     * Optional password for generating new credentials.
     * If not set, password from end entity is used.
     */
    private OptionalNull<String> password = OptionalNull.empty();

    /**
     * Recovers or generates new keys for the user and generates keystore.
     *
     * @param data
     *            user data for user
     * @param createJKS
     *            if a jks should be created
     * @param createPEM
     *            if pem files should be created
     * @param keyrecoverflag
     *            if we should try to revoer already existing keys
     * @throws Exception
     *             If something goes wrong...
     */
    public KeyStore generateClient(EndEntityInformation data, boolean createJKS, boolean createPEM,
                                   boolean keyrecoverflag, Long validity)
            throws CustomCertificateSerialNumberException, AuthStatusException, CAOfflineException,
            CertificateSerialNumberException, CryptoTokenOfflineException, CertificateRevokeException,
            ObjectNotFoundException, OperatorCreationException, AuthLoginException, IllegalKeyException,
            IOException, CertificateCreateException, VpnGenerationException, AuthorizationDeniedException,
            InvalidAlgorithmException, SignRequestSignatureException, IllegalNameException, CertificateException,
            IllegalValidityException, CADoesntExistsException, InvalidAlgorithmParameterException
    {
        final KeyPair rsaKeys = KeyTools.genKeys(VpnConfig.getKeySize(), VpnConfig.getKeySpec());

        // Get certificate for user and create keystore
        if (rsaKeys != null) {
            return createKeysForUser(
                    data.getUsername(),
                    password.orElse(data.getPassword()),
                    data.getCAId(),
                    rsaKeys, createJKS, createPEM,
                    !keyrecoverflag && data.getKeyRecoverable(),
                    null,
                    validity);
        }

        return null;
    }

    /**
     * Creates files for a user, sends request to CA, receives reply and creates P12.
     *
     * @param username
     *            username
     * @param password
     *            user's password
     * @param caid
     *            of CA used to issue the keystore certificates
     * @param rsaKeys
     *            a previously generated RSA keypair
     * @param createJKS
     *            if a jks should be created
     * @param createPEM
     *            if pem files should be created
     * @param savekeys
     *            if generated keys should be saved in db (key recovery)
     * @param orgCert
     *            if an original key recovered cert should be reused, null
     *            indicates generate new cert.
     * @throws VpnGenerationException
     *             if the certificate is not an X509 certificate
     * @throws VpnGenerationException
     *             if the CA-certificate is corrupt
     * @throws VpnGenerationException
     *             if verification of certificate or CA-cert fails
     * @throws VpnGenerationException
     *             if keyfile (generated by ourselves) is corrupt
     */
    public KeyStore createKeysForUser(String username, String password, int caid, KeyPair rsaKeys, boolean createJKS,
                                       boolean createPEM, boolean savekeys, X509Certificate orgCert, Long validity)
            throws VpnGenerationException, AuthorizationDeniedException, AuthLoginException,
            IllegalKeyException, CertificateCreateException, ObjectNotFoundException, CertificateRevokeException,
            CertificateSerialNumberException, CADoesntExistsException, IllegalValidityException,
            CustomCertificateSerialNumberException, CryptoTokenOfflineException, IllegalNameException,
            InvalidAlgorithmException, AuthStatusException, SignRequestSignatureException, CAOfflineException,
            CertificateException, OperatorCreationException, IOException
    {
        if (log.isTraceEnabled()) {
            log.trace(">createKeysForUser: username=" + username);
        }

        X509Certificate cert = null;
        if (orgCert != null) {
            cert = orgCert;
            boolean finishUser = getCaSession().getCAInfo(getAuthToken(), caid).getFinishUser();
            if (finishUser) {
                EndEntityInformation userdata = getEndEntityAccessSession().findUser(getAuthToken(), username);
                getEndEntityAuthenticationSession().finishUser(userdata);
            }

        } else {
            final String sigAlg = AlgorithmConstants.SIGALG_SHA256_WITH_RSA;
            final X509Certificate selfcert = CertTools.genSelfCert("CN=selfsigned",
                    validity != null ? validity : 3*365,
                    null, rsaKeys.getPrivate(), rsaKeys.getPublic(), sigAlg, false);

            cert = (X509Certificate) getSignSession().createCertificate(getAuthToken(), username, password, selfcert);
        }

        // Make a certificate chain from the certificate and the CA-certificate
        Certificate[] cachain = getSignSession().getCertificateChain(getAuthToken(), caid).toArray(new Certificate[0]);
        // Verify CA-certificate
        if (CertTools.isSelfSigned((X509Certificate) cachain[cachain.length - 1])) {
            try {
                // Make sure we have BC certs, otherwise SHA256WithRSAAndMGF1
                // will not verify (at least not as of jdk6).
                Certificate cacert = CertTools.getCertfromByteArray(cachain[cachain.length - 1].getEncoded());
                cacert.verify(cacert.getPublicKey());

            } catch (GeneralSecurityException se) {
                log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorrootnotverify"));
                throw new VpnGenerationException("vpn.errorrootnotverify");
            }
        } else {
            log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorrootnotselfsigned"));
            throw new VpnGenerationException("vpn.errorrootnotselfsigned");
        }

        // Verify that the user-certificate is signed by our CA
        try {
            // Make sure we have BC certs, otherwise SHA256WithRSAAndMGF1 will
            // not verify (at least not as of jdk6)
            Certificate cacert = CertTools.getCertfromByteArray(cachain[0].getEncoded());
            Certificate usercert = CertTools.getCertfromByteArray(cert.getEncoded());
            usercert.verify(cacert.getPublicKey());
        } catch (GeneralSecurityException se) {
            log.error(InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.errorgennotverify"));
            throw new VpnGenerationException("vpn.errorgennotverify");
        }

        // Use CN if as alias in the keystore, if CN is not present use username
        String alias = CertTools.getPartFromDN(CertTools.getSubjectDN(cert), "CN");
        if (alias == null) {
            alias = username;
        }

        // Store keys and certificates in keystore.
        KeyStore ks = null;
        try {
            if (createJKS) {
                ks = KeyTools.createJKS(alias, rsaKeys.getPrivate(), password, cert, cachain);
            } else {
                ks = KeyTools.createP12(alias, rsaKeys.getPrivate(), cert, cachain);
            }
        } catch (IOException | KeyStoreException | CertificateException | InvalidKeySpecException e) {
            throw new VpnGenerationException(e);
        } catch (NoSuchProviderException | NoSuchAlgorithmException e){
            throw new VpnGenerationException(e);
        } catch (Exception e){
            throw new VpnGenerationException(e);
        }

        //storeKeyStore(ks, username, password, createJKS, createPEM);
        String iMsg = InternalEjbcaResources.getInstance().getLocalizedMessage("vpn.createkeystore", username);
        log.info(iMsg);
        if (log.isTraceEnabled()) {
            log.trace("<createKeysForUser: username=" + username);
        }

        return ks;
    }

    // Getters / setters.

    public CaSession getCaSession() {
        if (caSession != null || !fetchRemoteSessions) {
            return caSession;
        }

        return getRemoteSession(CaSessionRemote.class);
    }

    public void setCaSession(CaSession caSession) {
        this.caSession = caSession;
    }

    public EndEntityAccessSession getEndEntityAccessSession() {
        if (endEntityAccessSession != null || !fetchRemoteSessions) {
            return endEntityAccessSession;
        }

        return getRemoteSession(EndEntityAccessSessionRemote.class);
    }

    public void setEndEntityAccessSession(EndEntityAccessSession endEntityAccessSession) {
        this.endEntityAccessSession = endEntityAccessSession;
    }

    public EndEntityAuthenticationSession getEndEntityAuthenticationSession() {
        if (endEntityAuthenticationSession != null || !fetchRemoteSessions) {
            return endEntityAuthenticationSession;
        }

        return getRemoteSession(EndEntityAuthenticationSessionRemote.class);
    }

    public void setEndEntityAuthenticationSession(EndEntityAuthenticationSession endEntityAuthenticationSession) {
        this.endEntityAuthenticationSession = endEntityAuthenticationSession;
    }

    public SignSession getSignSession() {
        if (signSession != null || !fetchRemoteSessions) {
            return signSession;
        }

        return getRemoteSession(SignSessionRemote.class);
    }

    public void setSignSession(SignSession signSession) {
        this.signSession = signSession;
    }

    public boolean isFetchRemoteSessions() {
        return fetchRemoteSessions;
    }

    public void setFetchRemoteSessions(boolean fetchRemoteSessions) {
        this.fetchRemoteSessions = fetchRemoteSessions;
    }

    public OptionalNull<String> getPassword() {
        return password;
    }

    /**
     * Sets optional password to use for creating a new keys - end entity authorisation.
     * @param password
     */
    public void setPassword(OptionalNull<String> password) {
        this.password = password;
    }
}
