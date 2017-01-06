package org.cesecore.vpn;

import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Misc VPN utils.
 * EJB level class.
 *
 * Created by dusanklinec on 06.01.17.
 */
public class VpnUtils {
    /**
     * Builds end entity user name from the VpnUser record.
     * @param user user to generate end entity from
     * @return end entity user name
     */
    public static String getUserName(VpnUser user){
        return user.getEmail() + "/" + user.getDevice();
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

}
