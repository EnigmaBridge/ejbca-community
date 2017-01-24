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
 
package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * P12toPEM is used to export PEM files from a single p12 file. The class exports the user
 * certificate, user private key in seperated files and the chain of sub ca and ca certifikate in
 * a third file. The PEM files will have the names <i>common name</i>.pem, <i>common
 * name</i>Key.pem and <i>common name</i>CA.pem derived from the DN in user certificate.
 *
 * @author ph4r05
 * @version $Id: P12toPEM.java 19901 2014-09-30 14:29:38Z anatom $
 */
public class P12toPEM {
    private static Logger log = Logger.getLogger(P12toPEM.class);
    private String exportpath = "./p12/pem/";
    private String p12File;
    private String password;
    private String userName;
    private String fileNameBase;
    private KeyStore ks = null;
    boolean overwrite = false;

    private static final byte[] beginCertificate = "-----BEGIN CERTIFICATE-----".getBytes();
    private static final byte[] endCertificate = "-----END CERTIFICATE-----".getBytes();
    private static final byte[] beginPrivateKey = "-----BEGIN PRIVATE KEY-----".getBytes();
    private static final byte[] endPrivateKey = "-----END PRIVATE KEY-----".getBytes();
    private static final byte[] NL = "\n".getBytes();

    /**
     * Basic construtor for the P12toPEM class, set variables for the class.
     *
     * @param p12File p12File The (path +) name of the input p12 file.
     * @param password password The password for the p12 file.
     * 
     */
    public P12toPEM(String p12File, String password) {
        this.p12File = p12File;
        this.password = password;
    }

	/**
	 * Basic constructor using a in memory KeyStore instead for a file.
	 *
	 * @param keystore the KeyStore to use.
	 * @param password password The password for the p12 file.
	 * @param overwrite overwrite If existing files should be overwritten.    
	 */
	public P12toPEM(KeyStore keystore, String password, boolean overwrite) {		
		this.password = password;
		this.ks = keystore;
		this.overwrite = overwrite;
	}


    /**
     * Sets the directory where PEM-files wil be stores
     *
     * @param path path where PEM-files will be stores
     */
    public void setExportPath(String path) {
        exportpath = path;
    }

    public String getFileNameBase() {
        return fileNameBase;
    }

    /**
     * Set file name base of the files being generated.
     * @param fileNameBase
     */
    public void setFileNameBase(String fileNameBase) {
        this.fileNameBase = fileNameBase;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Constructor for the P12toPEM class.
     *
     * @param p12File p12File The (path +) name of the input p12 file.
     * @param password password The password for the p12 file.
     * @param overwrite overwrite If existing files should be overwritten.
     */
    public P12toPEM(String p12File, String password, boolean overwrite) {
        this.p12File = p12File;
        this.password = password;
        this.overwrite = overwrite;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws KeyStoreException DOCUMENT ME!
     * @throws FileNotFoundException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     * @throws NoSuchProviderException DOCUMENT ME!
     * @throws NoSuchAlgorithmException DOCUMENT ME!
     * @throws CertificateEncodingException DOCUMENT ME!
     * @throws CertificateException DOCUMENT ME!
     * @throws UnrecoverableKeyException DOCUMENT ME!
     */
    public void createPEM()
        throws KeyStoreException, FileNotFoundException, IOException, NoSuchProviderException, 
            NoSuchAlgorithmException, CertificateEncodingException, CertificateException, 
            UnrecoverableKeyException {
         
         if(this.ks == null){
            ks = KeyStore.getInstance("PKCS12", "BC");
            InputStream in = new FileInputStream(p12File);
            ks.load(in, password.toCharArray());
            in.close();
        }

        // Fid the key private key entry in the keystore
        final Enumeration<String> e = ks.aliases();
        Object o = null;
        PrivateKey serverPrivKey = null;

        while (e.hasMoreElements()) {
            o = e.nextElement();
            if (o == null){
                continue;
            }

            if ((ks.isKeyEntry((String) o)) &&
                    ((serverPrivKey = (PrivateKey) ks.getKey((String) o, password.toCharArray())) != null)) {
                log.debug("Aliases " + o + " is KeyEntry.");

                break;
            }
        }

        log.debug("Private key encode: " + (serverPrivKey == null ? null : serverPrivKey.getFormat()));
        byte[] privKeyEncoded = "".getBytes();
        if (serverPrivKey != null) {
            privKeyEncoded = serverPrivKey.getEncoded();
        }

        Certificate[] chain = KeyTools.getCertChain(ks, (String) o);
        log.debug("Loaded certificate chain with length " + chain.length + " from keystore.");

        X509Certificate userX509Certificate = (X509Certificate) chain[0];
        final byte[] certBytes = userX509Certificate.getEncoded();
        final String dname = CertTools.getSubjectDN(userX509Certificate);
        final String cname = CertTools.getPartFromDN(dname, "CN");
        String userFileTmp = null;
        if (fileNameBase != null){
            userFileTmp = fileNameBase;
        } else if (userName != null){
            userFileTmp = userName + "_" + cname;
        } else {
            userFileTmp = cname;
        }

        final String userFile = VpnUtils.sanitizeFileName(userFileTmp);
        final String filetype = ".pem";

        File path = new File(exportpath);
        path.mkdir();

        File tmpFile = new File(path, userFile + filetype);

        if (!overwrite && tmpFile.exists()) {
            log.error("File '" + tmpFile + "' already exists, don't overwrite.");
            return;
        }

        OutputStream out = new FileOutputStream(tmpFile);
        out.write(beginCertificate);
        out.write(NL);
        out.write(Base64.encode(certBytes));
        out.write(NL);
        out.write(endCertificate);
        out.close();

        tmpFile = new File(path, userFile + "-key" + filetype);
        if (!overwrite && tmpFile.exists()) {
            log.error("File '" + tmpFile + "' already exists, don't overwrite.");
            return;
        }

        // Readable & writable only by the owner.
        tmpFile.createNewFile();
        VpnUtils.readOwnerOnly(tmpFile);

        out = new FileOutputStream(tmpFile);
        VpnUtils.readOwnerOnly(tmpFile);
        
        out.write(beginPrivateKey);
        out.write(NL);
        out.write(Base64.encode(privKeyEncoded));
        out.write(NL);
        out.write(endPrivateKey);
        out.close();

        tmpFile = new File(path, userFile + "-CA" + filetype);
        if (!overwrite && tmpFile.exists()) {
            log.error("File '" + tmpFile + "' already exists, don't overwrite.");
            return;
        }

        if (CertTools.isSelfSigned(userX509Certificate)) {
            log.info("User certificate is selfsigned, this is a RootCA, no CA certificates written.");
        } else {
            out = new FileOutputStream(tmpFile);

            for (int num = 1; num < chain.length; num++) {
                final X509Certificate tmpX509Cert = (X509Certificate) chain[num];

                out.write(beginCertificate);
                out.write(NL);
                out.write(Base64.encode(tmpX509Cert.getEncoded()));
                out.write(NL);
                out.write(endCertificate);
                out.write(NL);
            }
            out.close();
        }
    }

}
