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
package org.cesecore.keybind.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.OperatorCreationException;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.keybind.CertificateImportException;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Test of OcspKeyBinding implementation.
 * 
 * @version $Id: OcspKeyBindingTest.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class OcspKeyBindingTest {

    private static List<Extension> ekuExtensionOnly;

    // Define a traceLogMethodsRule similar to the system tests TraceLogMethodsRule() implementation.
    @Rule
    public TestRule traceLogMethodsRule = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            final Logger log = Logger.getLogger(description.getClassName());
            if (log.isTraceEnabled()) {
                log.trace(">" + description.getMethodName());
            }
            super.starting(description);
        };
        @Override
        protected void finished(Description description) {
            final Logger log = Logger.getLogger(description.getClassName());
            if (log.isTraceEnabled()) {
                log.trace("<" + description.getMethodName());
            }
            super.finished(description);
        }
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        CryptoProviderTools.installBCProvider();
        ekuExtensionOnly = Arrays.asList(new Extension[] { getExtendedKeyUsageExtension() });
    }

    @Test
    public void testOcspSigningCertificateValidationPositives() throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, IllegalStateException, NoSuchProviderException, OperatorCreationException, CertificateException {
        assertTrue("KU=digitalSignature and EKU=id_kp_OCSPSigning should be treated as a valid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.digitalSignature, ekuExtensionOnly)));
        assertTrue("KU=digitalSignature and EKU=id_kp_OCSPSigning should be treated as a valid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.digitalSignature + X509KeyUsage.cRLSign, ekuExtensionOnly)));
        assertTrue("KU=nonRepudiation and EKU=id_kp_OCSPSigning should be treated as a valid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.nonRepudiation + X509KeyUsage.cRLSign, ekuExtensionOnly)));
        assertTrue("KU=digitalSignature+nonRepudiation and EKU=id_kp_OCSPSigning should be treated as a valid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.digitalSignature + X509KeyUsage.nonRepudiation, ekuExtensionOnly)));
    }

    @Test
    public void testOcspSigningCertificateAssertionPositives() throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, IllegalStateException, NoSuchProviderException, OperatorCreationException, CertificateException {
        try {
            new OcspKeyBinding().assertCertificateCompatability(getCertificate(X509KeyUsage.digitalSignature, ekuExtensionOnly));
        } catch (CertificateImportException e) {
            fail("KU=digitalSignature and EKU=id_kp_OCSPSigning should be treated as a valid OCSP singing certificate.");
        }
    }

    @Test
    public void testOcspSigningCertificateValidationNegatives() throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, IllegalStateException, NoSuchProviderException, OperatorCreationException, CertificateException {
        assertFalse("KU!=digitalSignature|nonRepudiation and EKU=id_kp_OCSPSigning should be treated as an invalid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.keyAgreement + X509KeyUsage.cRLSign, ekuExtensionOnly)));
        assertFalse("KU=digitalSignature and EKU!=id_kp_OCSPSigning should be treated as an invalid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.digitalSignature, null)));
        assertFalse("KU=nonRepudiation and EKU!=id_kp_OCSPSigning should be treated as an invalid OCSP singing certificate.",
                OcspKeyBinding.isOcspSigningCertificate(getCertificate(X509KeyUsage.nonRepudiation, null)));
 }

    @Test
    public void testOcspSigningCertificateAssertionNegatives() throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, IllegalStateException, NoSuchProviderException, OperatorCreationException, CertificateException {
        try {
            new OcspKeyBinding().assertCertificateCompatability(getCertificate(X509KeyUsage.cRLSign, null));
            fail("KU=cRLSign and EKU!=id_kp_OCSPSigning should be treated as an invalid OCSP singing certificate.");
        } catch (CertificateImportException e) {
            // Expected outcome
        }
    }

    /** @return A self-signed certificate. */
    private X509Certificate getCertificate(final int keyUsage, final List<Extension> extensions) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException,
        SignatureException, IllegalStateException, NoSuchProviderException, OperatorCreationException, CertificateException, IOException {
        final KeyPair keyPair = KeyTools.genKeys("512", AlgorithmConstants.KEYALGORITHM_RSA);
        return CertTools.genSelfCertForPurpose("CN=OcspSinger", 365, null, keyPair.getPrivate(), keyPair.getPublic(), "SHA256WithRSA", false, keyUsage, null, null, "BC", true, extensions);
    }

    /** @return An extended key usage extension with id_kp_OCSPSigning set. */
    private static Extension getExtendedKeyUsageExtension() throws IOException {
        final ASN1Encodable usage = KeyPurposeId.getInstance(KeyPurposeId.id_kp_OCSPSigning);
        final ASN1Sequence seq = ASN1Sequence.getInstance(new DERSequence(usage));
        return new Extension(Extension.extendedKeyUsage, true, seq.getEncoded());
    }

}
