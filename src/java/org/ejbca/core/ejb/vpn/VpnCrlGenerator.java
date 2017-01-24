package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSession;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.util.CertTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Generates CRL. Used in the WEB / CLI.
 *
 * @author ph4r05
 * Created by dusanklinec on 24.01.17.
 */
public class VpnCrlGenerator extends VpnBaseHelper {
    private static final Logger log = Logger.getLogger(VpnCrlGenerator.class);
    private VpnUserManagementSession vpnSession;
    private CaSession caSession;

    private SecureRandom random = new SecureRandom();

    private boolean force = true;
    private boolean write = true;
    private boolean der = false;
    private File crlDirectory = null;
    private Boolean crlUseMoveStrategy;

    private byte[] crlDer;
    private String crlPem;
    private File crlPath;
    private Integer crlId;

    /**
     * Builds CRL and updates the corresponding file.
     *
     * @throws VpnException Generic VPN mgmt call error
     * @throws CADoesntExistsException VPN CA does not exist
     * @throws AuthorizationDeniedException not authorized to perform CRL operation on the CA
     * @throws CertificateException
     * @throws NoSuchProviderException BC missing
     * @throws CRLException CRL processing error (pem der conversion)
     * @throws IOException typically file operation failed
     */
    public Integer generate() throws VpnException, CADoesntExistsException, AuthorizationDeniedException, CertificateException, NoSuchProviderException, CRLException, IOException {
        crlId = generate(isForce(), isWrite());
        return crlId;
    }

    /**
     * Builds CRL and updates the corresponding file.
     *
     * @param force if true the CRL is generated even if the previous one is still valid.
     * @param write if true the corresponding CRL file is updated on the FS.
     * @throws VpnException Generic VPN mgmt call error
     * @throws CADoesntExistsException VPN CA does not exist
     * @throws AuthorizationDeniedException not authorized to perform CRL operation on the CA
     * @throws IOException typically file operation failed
     * @throws CertificateException
     * @throws CRLException CRL processing error (pem der conversion)
     * @throws NoSuchProviderException BC missing
     */
    public Integer generate(boolean force, boolean write) throws VpnException, CADoesntExistsException, AuthorizationDeniedException, IOException, CertificateException, CRLException, NoSuchProviderException {
        crlId = getVpnSession().generateCRL(getAuthToken(), force, null);
        crlDer = getVpnSession().getCRL(getAuthToken());
        crlPem = VpnUtils.crlDerToPem(crlDer);
        final boolean useMoveStrategy = crlUseMoveStrategy != null ? crlUseMoveStrategy : VpnConfig.shouldUseMoveForCrlGeneration();

        if (write){
            final CAInfo vpnCa = getCa();
            final String cn = CertTools.getPartFromDN(vpnCa.getSubjectDN(), "CN");

            if (crlDirectory != null) {
                crlDirectory.mkdirs();
            }

            final String crlFileName = VpnUtils.sanitizeFileName(cn + ".crl");
            crlPath = new File(crlDirectory != null ? crlDirectory : VpnConfig.getCrlDirectory(), crlFileName);
            File crlPathTmp = crlPath;

            if (useMoveStrategy) {
                final String crlFileNameTmp = VpnUtils.sanitizeFileName(cn + ".crl." + Math.abs(random.nextLong()));
                crlPathTmp = new File(VpnConfig.getCrlDirectory(), crlFileNameTmp);
            }

            final FileOutputStream fos = new FileOutputStream(crlPathTmp);
            fos.write(isDer() ? crlDer : crlPem.getBytes("UTF-8"));
            fos.close();

            // Move temp file to the target file.
            if (useMoveStrategy) {
                Files.move(
                        Paths.get(crlPathTmp.getAbsolutePath()),
                        Paths.get(crlPath.getAbsolutePath()),
                        REPLACE_EXISTING);
            }
        }

        return crlId;
    }

    protected CAInfo getCa() throws AuthorizationDeniedException, CADoesntExistsException {
        return getCaSession().getCAInfo(getAuthToken(), VpnConfig.getCA());
    }

    // Getters / setters.

    public boolean isWrite() {
        return write;
    }

    public VpnCrlGenerator setWrite(boolean write) {
        this.write = write;
        return this;
    }

    public boolean isForce() {
        return force;
    }

    public VpnCrlGenerator setForce(boolean force) {
        this.force = force;
        return this;
    }

    public boolean isDer() {
        return der;
    }

    public Boolean getCrlUseMoveStrategy() {
        return crlUseMoveStrategy;
    }

    public VpnCrlGenerator setCrlUseMoveStrategy(Boolean crlUseMoveStrategy) {
        this.crlUseMoveStrategy = crlUseMoveStrategy;
        return this;
    }

    public VpnCrlGenerator setDer(boolean der) {
        this.der = der;
        return this;
    }

    public File getCrlDirectory() {
        return crlDirectory;
    }

    public VpnCrlGenerator setCrlDirectory(File crlDirectory) {
        this.crlDirectory = crlDirectory;
        return this;
    }

    public VpnUserManagementSession getVpnSession() {
        if (vpnSession != null || !fetchRemoteSessions) {
            return vpnSession;
        }

        return getRemoteSession(VpnUserManagementSessionRemote.class);
    }

    public VpnCrlGenerator setVpnSession(VpnUserManagementSession vpnSession) {
        this.vpnSession = vpnSession;
        return this;
    }

    public CaSession getCaSession() {
        if (caSession != null || !fetchRemoteSessions) {
            return caSession;
        }

        return getRemoteSession(CaSessionRemote.class);
    }

    public VpnCrlGenerator setCaSession(CaSession caSession) {
        this.caSession = caSession;
        return this;
    }

    public boolean isFetchRemoteSessions() {
        return fetchRemoteSessions;
    }

    public VpnCrlGenerator setFetchRemoteSessions(boolean fetchRemoteSessions) {
        this.fetchRemoteSessions = fetchRemoteSessions;
        return this;
    }

    public byte[] getCrlDer() {
        return crlDer;
    }

    public String getCrlPem() {
        return crlPem;
    }

    public File getCrlPath() {
        return crlPath;
    }

    public Integer getCrlId() {
        return crlId;
    }
}
