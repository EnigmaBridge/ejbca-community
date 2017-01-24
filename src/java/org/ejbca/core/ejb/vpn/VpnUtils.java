package org.ejbca.core.ejb.vpn;

import org.apache.commons.validator.routines.EmailValidator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.StringTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.util.passgen.IPasswordGenerator;
import org.ejbca.util.passgen.PasswordGeneratorFactory;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Misc VPN utils.
 * EJB level class.
 *
 * @author ph4r05
 * Created by dusanklinec on 06.01.17.
 */
public class VpnUtils {
    /**
     * Builds end entity user name from the VpnUser record.
     * @param user user to generate end entity from
     * @return end entity user name
     */
    public static String getUserName(VpnUser user){
        return StringTools.stripUsername(user.getEmail() + "/" + user.getDevice());
    }

    /**
     * Converts Private key to non-encrypted PEM
     * @param key private key to convert to PEM
     * @return PEM as a string
     * @throws IOException
     */
    public static String privateKeyToPem(PrivateKey key) throws IOException {
        final CharArrayWriter charWriter = new CharArrayWriter();
        final JcaPKCS8Generator generator = new JcaPKCS8Generator(key, null);

        PemWriter writer = new PemWriter(charWriter);
        writer.writeObject(generator);
        writer.close();

        return charWriter.toString();
    }

    /**
     * Converts certificate to PEM string
     * @param certificate certificate to convert to PEM
     * @return PEM as a string
     * @throws IOException
     */
    public static String certificateToPem(Certificate certificate) throws IOException {
        final CharArrayWriter charWriter = new CharArrayWriter();
        final JcaMiscPEMGenerator generator = new JcaMiscPEMGenerator(certificate, null);

        PemWriter writer = new PemWriter(charWriter);
        writer.writeObject(generator);
        writer.close();

        return charWriter.toString();
    }

    /**
     * Returns true if given email is valid.
     * @param email
     * @return
     */
    public static boolean isEmailValid(String email){
        if (email == null || email.isEmpty()){
            return false;
        }

        return EmailValidator.getInstance().isValid(email);
    }

    /**
     * Returns Common name for the user name
     * @param name user
     * @return CommonName
     */
    public static String genUserCN(String name){
        return "CN="+ StringTools.stripUsername(name);
    }

    /**
     * Returns Common name for the user
     * @param user user
     * @return CommonName
     */
    public static String genUserCN(VpnUser user){
        return genUserCN(getUserName(user));
    }

    /**
     * Returns SubjectAltName for the VpnUser.
     * @param user user
     * @return SubjectAltName
     */
    public static String genUserAltName(VpnUser user){
        return "rfc822name="+user.getEmail();
    }

    /**
     * Adds key store to the VpnUser - sets appropriate fields
     * @param vpnUser vpn user record
     * @param ks KeyStore with cert & private key
     * @param password KeyStore password
     * @return
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static VpnUser addKeyStoreToUser(VpnUser vpnUser, KeyStore ks, char[] password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        // Store KS to the database
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ks.store(bos, password);
        vpnUser.setKeyStore(new String(Base64.encode(bos.toByteArray()), "UTF-8"));

        // Extract certificate & fingerprint
        final Certificate cert = ks.getCertificate(getUserName(vpnUser));
        final String certFprint = CertTools.getFingerprintAsString(cert);
        vpnUser.setCertificateId(certFprint);
        vpnUser.setCertificate(new String(Base64.encode(cert.getEncoded())));
        vpnUser.setDateModified(System.currentTimeMillis());
        vpnUser.setRevokedStatus(0);

        return vpnUser;
    }

    /**
     * Generated a random OTP password
     *
     * @return a randomly generated password
     */
    public static String genRandomPwd() {
        final IPasswordGenerator pwdgen = PasswordGeneratorFactory.getInstance(PasswordGeneratorFactory.PASSWORDTYPE_NOSOUNDALIKEENLD);
        return pwdgen.getNewPassword(24, 24);
    }

    /**
     * Sanitizes single file name
     * @param fileName
     * @return
     */
    public static String sanitizeFileName(String fileName){
        fileName = StringTools.stripFilename(fileName);
        fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        fileName = fileName.replaceAll("[_]{2,}", "_");
        return fileName;
    }

    /**
     * Converts resource bundle to properties.
     * @param resource
     * @return
     */
    public static Properties convertResourceBundleToProperties(ResourceBundle resource) {
        final Properties properties = new Properties();
        final Enumeration<String> keys = resource.getKeys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            properties.put(key, resource.getString(key));
        }

        return properties;
    }

    /**
     * Extracts CN from the DN of the certificate.
     * @param certificate certificate to extract CN from
     * @return CN from DN from the certificate
     */
    public static String extractCN(Certificate certificate){
        final String certDn = CertTools.getSubjectDN(certificate);
        return CertTools.getPartFromDN(certDn, "CN");
    }

    /**
     * Returns top hostname domain, e.g., from blackburn.enigmabridge.com -> blackburn
     * @param hostname hostname to process or null.
     * @return domain or null
     */
    public static String getHostnameId(String hostname){
        if (hostname == null){
            return hostname;
        }

        final String[] parts = hostname.split("\\.", 2);
        return parts[0];
    }

    /**
     * Generates a file name for the ovpn file
     * @param user
     * @return
     */
    public static String genVpnConfigFileName(VpnUser user){
        final String settingHostname = VpnConfig.getServerHostname();
        final String hostPart = getHostnameId(settingHostname);

        String fileName = String.format("%s_%s", user.getEmail(), user.getDevice());
        if (hostPart != null){
            fileName += "_" + hostPart;
        }

        final SimpleDateFormat formatter = new SimpleDateFormat("YYYYMMdd", Locale.getDefault());
        final String dateFmted = formatter.format(
                new Date(user.getConfigGenerated() == null ? System.currentTimeMillis() : user.getConfigGenerated()));
        fileName += "_" + dateFmted;
        fileName += "_v" + user.getConfigVersion();
        fileName += ".ovpn";

        fileName = VpnUtils.sanitizeFileName(fileName);
        return fileName;
    }

    /**
     * Converts properties to a JSON.
     * @param properties
     * @return
     */
    public static JSONObject properties2json(Properties properties){
        final JSONObject ret = new JSONObject();
        if (properties == null || properties.isEmpty()) {
            return ret;
        }

        Enumeration<?> enumProperties = properties.propertyNames();
        while(enumProperties.hasMoreElements()) {
            final Object keyObj = enumProperties.nextElement();
            if (!(keyObj instanceof String)){
                continue;
            }

            String name = (String) keyObj;
            ret.put(name, properties.getProperty(name));
        }

        return ret;
    }

    /**
     * Sets file to read only for the owner
     * @param file file to set parameters to.
     */
    public static boolean readOwnerOnly(File file){
        boolean result = file.setReadable(false, false);
        result &= file.setWritable(false, false);
        result &= file.setExecutable(false, false);
        result &= file.setReadable(true, true);
        result &= file.setWritable(true, true);
        return result;
    }
}
