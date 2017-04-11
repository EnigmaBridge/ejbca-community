package org.ejbca.core.ejb.vpn;

import org.apache.commons.validator.routines.EmailValidator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.StringTools;
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;
import org.ejbca.util.passgen.IPasswordGenerator;
import org.ejbca.util.passgen.PasswordGeneratorFactory;
import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

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
     * Builds CRL object from the DER representation.
     *
     * @param crlData der encoded CRL
     * @return X509CRL
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws CRLException
     */
    public static X509CRL buildCrl(byte[] crlData) throws CertificateException, NoSuchProviderException, CRLException {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        return (X509CRL) cf.generateCRL(new ByteArrayInputStream(crlData));
    }

    /**
     * Converts DER encoded CRL to PEM encoded CRL
     * @param crlData der encoded CRL
     * @return PEM encoded CRL
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws CRLException
     * @throws IOException
     */
    public static String crlDerToPem(byte[] crlData) throws CertificateException, NoSuchProviderException, CRLException, IOException {
        final X509CRL x509CRL = buildCrl(crlData);

        final CharArrayWriter charWriter = new CharArrayWriter();
        final JcaMiscPEMGenerator generator = new JcaMiscPEMGenerator(x509CRL, null);
        final PemWriter writer = new PemWriter(charWriter);

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
     * @param fileName filename to sanitize
     * @return sanitized file name
     */
    public static String sanitizeFileName(String fileName){
        return sanitizeFileName(fileName, false, "_");
    }

    /**
     * Sanitizes single file name
     * @param fileName filename to sanitize
     * @param allowSpace if true a space is allowed in the file name
     * @param separator default separator for invalid characters
     * @return sanitized file name
     */
    public static String sanitizeFileName(String fileName, boolean allowSpace, String separator){
        fileName = StringTools.stripFilename(fileName);
        if (allowSpace){
            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_\\s]", separator);
        } else {
            fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", separator);
        }
        fileName = fileName.replaceAll("[_]{2,}", "_");

        if (!separator.isEmpty()) {
            fileName = fileName.replaceAll("[" + Pattern.quote(separator) + "]{2,}", separator);
        }
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
     * @return domain or null
     */
    public static String getHostnameId(){
        return getHostnameId(VpnConfig.getServerHostname());
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
     * @param user vpn user to generate filename for
     * @return OpenVPN configuration file name
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
     * Generates a file name for the ovpn file - more human friendly
     * @param user vpn user to generate filename for
     * @param genOptions options for generating the file name
     * @return human friendly OpenVPN configuration file name
     */
    public static String genVpnConfigFileNameHuman(VpnUser user, VpnGenOptions genOptions){
        final String settingHostname = VpnConfig.getServerHostname();
        final String hostPart = getHostnameId(settingHostname);

        String fileName = String.format("Private Space %s - %s - %s",
                hostPart, user.getDevice(), user.getEmail().replace("@", "_"));

        final SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd", Locale.getDefault());
        final String dateFmted = formatter.format(
                new Date(user.getConfigGenerated() == null ? System.currentTimeMillis() : user.getConfigGenerated()));
        fileName += " " + dateFmted;
        fileName += " v" + user.getConfigVersion();
        fileName += ".ovpn";

        fileName = VpnUtils.sanitizeFileName(fileName, true, "_");

        // If Android, remove white spaces
        if (genOptions != null && genOptions.getOs() != null){
            final OperatingSystem group = genOptions.getOs().getGroup();
            if (OperatingSystem.ANDROID.equals(group)){
                fileName = fileName.replace(" ", "");
            }
        }
        return fileName;
    }

    /**
     * Generates a filename for the token file - human friendly
     * @param token token
     * @return human friendly p12 configuration file name
     */
    public static String getP12FileNameHuman(OtpDownload token){
        return String.format("%s-%s.p12", VpnUtils.getHostnameId(), VpnUtils.sanitizeFileName(token.getOtpId()));
    }

    /**
     * Converts string with \r\n or \n line endings to the \r\n line ending
     * @param data input data to process to
     * @return output converted string
     */
    public static String toWindowsEOL(String data){
        // Normalize all possible line endings
        data = data.replaceAll("\\r\\n", "\n");
        data = data.replaceAll("\\r", "\n");
        // Transform to windows line ending
        data = data.replaceAll("\\n", "\r\n");
        return data;
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

    /**
     * Builds direct download link for the ovpn config
     * @param user vpn user
     * @return
     */
    public static String getDirectDownloadLink(VpnUser user){
        return "https://" + VpnConfig.getServerHostname() + ":" + VpnConfig.getPublicHttpsPort() + "/"
                + String.format("ejbca/vpn/getvpn?id=%s&otp=%s", user.getId(), user.getOtpDownload());
    }

    /**
     * Returns true if the given IP address is inside VPN address range.
     * @param ip
     * @return true if given IP belongs to the VPN address range
     */
    public static boolean isIpInVPNNetwork(String ip) throws UnknownHostException{
        final String vpnNetAddr = VpnConfig.getConfigVpnSubnetAddress();
        final int vpnNetSize = VpnConfig.getConfigVpnSubnetSize();

        final Subnet subnet = new Subnet(InetAddress.getByName(vpnNetAddr), vpnNetSize);
        return subnet.isInNet(InetAddress.getByName(ip));
    }

    /**
     * Subnet class for checking whether IP address belongs to the interval
     * https://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask
     */
    public static class Subnet {
        final private int bytesSubnetCount;
        final private BigInteger bigMask;
        final private BigInteger bigSubnetMasked;

        /**
         * For use via format "192.168.0.0/24" or "2001:db8:85a3:880:0:0:0:0/57"
         */
        public Subnet(final InetAddress subnetAddress, final int bits) {
            this.bytesSubnetCount = subnetAddress.getAddress().length; // 4 or 16
            this.bigMask = BigInteger.valueOf(-1).shiftLeft(this.bytesSubnetCount * 8 - bits); // mask = -1 << 32 - bits
            this.bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(this.bigMask);
        }

        /**
         * For use via format "192.168.0.0/255.255.255.0" or single address
         */
        public Subnet(final InetAddress subnetAddress, final InetAddress mask) {
            this.bytesSubnetCount = subnetAddress.getAddress().length;
            this.bigMask = null == mask ? BigInteger.valueOf(-1) : new BigInteger(mask.getAddress()); // no mask given case is handled here.
            this.bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(this.bigMask);
        }

        /**
         * Subnet factory method.
         *
         * @param subnetMask format: "192.168.0.0/24" or "192.168.0.0/255.255.255.0"
         *                   or single address or "2001:db8:85a3:880:0:0:0:0/57"
         * @return a new instance
         * @throws UnknownHostException thrown if unsupported subnet mask.
         */
        public static Subnet createInstance(final String subnetMask) throws UnknownHostException {
            final String[] stringArr = subnetMask.split("/");
            if (2 > stringArr.length) {
                return new Subnet(InetAddress.getByName(stringArr[0]), (InetAddress) null);
            } else if (stringArr[1].contains(".") || stringArr[1].contains(":")) {
                return new Subnet(InetAddress.getByName(stringArr[0]), InetAddress.getByName(stringArr[1]));
            } else {
                return new Subnet(InetAddress.getByName(stringArr[0]), Integer.parseInt(stringArr[1]));
            }
        }

        public boolean isInNet(final InetAddress address) {
            final byte[] bytesAddress = address.getAddress();
            if (this.bytesSubnetCount != bytesAddress.length) {
                return false;
            }
            final BigInteger bigAddress = new BigInteger(bytesAddress);
            return bigAddress.and(this.bigMask).equals(this.bigSubnetMasked);
        }

        @Override
        final public boolean equals(Object obj) {
            if (!(obj instanceof Subnet)) {
                return false;
            }
            final Subnet other = (Subnet) obj;
            return this.bigSubnetMasked.equals(other.bigSubnetMasked) &&
                    this.bigMask.equals(other.bigMask) &&
                    this.bytesSubnetCount == other.bytesSubnetCount;
        }

        @Override
        final public int hashCode() {
            return this.bytesSubnetCount;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            bigInteger2IpString(buf, this.bigSubnetMasked, this.bytesSubnetCount);
            buf.append('/');
            bigInteger2IpString(buf, this.bigMask, this.bytesSubnetCount);
            return buf.toString();
        }

        static private void bigInteger2IpString(final StringBuilder buf, final BigInteger bigInteger, final int displayBytes) {
            final boolean isIPv4 = 4 == displayBytes;
            byte[] bytes = bigInteger.toByteArray();
            int diffLen = displayBytes - bytes.length;
            final byte fillByte = 0 > (int) bytes[0] ? (byte) 0xFF : (byte) 0x00;

            int integer;
            for (int i = 0; i < displayBytes; i++) {
                if (0 < i && !isIPv4 && i % 2 == 0)
                    buf.append(':');
                else if (0 < i && isIPv4)
                    buf.append('.');
                integer = 0xFF & (i < diffLen ? fillByte : bytes[i - diffLen]);
                if (!isIPv4 && 0x10 > integer)
                    buf.append('0');
                buf.append(isIPv4 ? integer : Integer.toHexString(integer));
            }
        }
    }
}
