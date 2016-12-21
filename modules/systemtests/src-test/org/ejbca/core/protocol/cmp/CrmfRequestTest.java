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

package org.ejbca.core.protocol.cmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.ejb.FinderException;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIHeader;
import org.bouncycastle.asn1.cmp.PKIHeaderBuilder;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.jce.X509KeyUsage;
import org.cesecore.CaTestUtils;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CAConstants;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.ca.X509CAInfo;
import org.cesecore.certificates.ca.catoken.CAToken;
import org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceInfo;
import org.cesecore.certificates.certificate.InternalCertificateStoreSessionRemote;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.configuration.GlobalConfigurationSessionRemote;
import org.cesecore.keys.token.CryptoTokenTestUtils;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.cesecore.util.StringTools;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.KeyRecoveryCAServiceInfo;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * This test runs in 'normal' CMP mode
 * 
 * @version $Id: CrmfRequestTest.java 19974 2014-10-09 15:19:06Z mikekushner $
 * 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CrmfRequestTest extends CmpTestCase {

    private static final Logger log = Logger.getLogger(CrmfRequestTest.class);

    private static final String USER = "abc123rry" + new Random().nextLong();
    private final static X500Name USER_DN = new X500Name("CN=" + USER + ", O=PrimeKey Solutions AB, C=SE");
    private final static String ISSUER_DN = "CN=TestCA";
    private final KeyPair keys;
    private final int caid;
    private final X509Certificate cacert;
    private final CA testx509ca;
    private final CmpConfiguration cmpConfiguration;
    private final static String cmpAlias = "CrmfRequestTestCmpConfigAlias";

    private final CaSessionRemote caSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class);
    private final GlobalConfigurationSessionRemote globalConfigurationSession = EjbRemoteHelper.INSTANCE.getRemoteSession(GlobalConfigurationSessionRemote.class);
    private final InternalCertificateStoreSessionRemote internalCertStoreSession = EjbRemoteHelper.INSTANCE.getRemoteSession(InternalCertificateStoreSessionRemote.class, EjbRemoteHelper.MODULE_TEST);

    @BeforeClass
    public static void beforeClass() {
        CryptoProviderTools.installBCProvider();
    }

    public CrmfRequestTest() throws Exception {
        this.cmpConfiguration = (CmpConfiguration) this.globalConfigurationSession.getCachedConfiguration(CmpConfiguration.CMP_CONFIGURATION_ID);
        
        int keyusage = X509KeyUsage.digitalSignature + X509KeyUsage.keyCertSign + X509KeyUsage.cRLSign;
        this.testx509ca = CaTestUtils.createTestX509CA(ISSUER_DN, null, false, keyusage);
        this.caid = this.testx509ca.getCAId();
        this.cacert = (X509Certificate) this.testx509ca.getCACertificate();
        this.keys = KeyTools.genKeys("512", AlgorithmConstants.KEYALGORITHM_RSA);
    }
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.caSession.addCA(ADMIN, this.testx509ca);
        
        log.debug("ISSUER_DN: " + ISSUER_DN);
        log.debug("caid: " + this.caid);
        
        this.cmpConfiguration.addAlias(cmpAlias);
        this.cmpConfiguration.setRAMode(cmpAlias, false);
        this.cmpConfiguration.setResponseProtection(cmpAlias, "signature");
        this.cmpConfiguration.setCMPDefaultCA(cmpAlias, ISSUER_DN);
        this.cmpConfiguration.setAuthenticationModule(cmpAlias, CmpConfiguration.AUTHMODULE_REG_TOKEN_PWD + ";" + CmpConfiguration.AUTHMODULE_HMAC);
        this.cmpConfiguration.setAuthenticationParameters(cmpAlias, "-;foo123");
        this.cmpConfiguration.setExtractUsernameComponent(cmpAlias, "CN");
        this.cmpConfiguration.setRACertProfile(cmpAlias, CP_DN_OVERRIDE_NAME);
        this.cmpConfiguration.setRAEEProfile(cmpAlias, EEP_DN_OVERRIDE_NAME);
        this.globalConfigurationSession.saveConfiguration(ADMIN, this.cmpConfiguration);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
        CryptoTokenTestUtils.removeCryptoToken(null, this.testx509ca.getCAToken().getCryptoTokenId());
        this.caSession.removeCA(ADMIN, this.caid);
        
        try {
            this.endEntityManagementSession.deleteUser(ADMIN, "cmptest");
        } catch (NotFoundException e) {
            // A test probably failed before creating the entity
            log.debug("Failed to delete USER \"cmptest\".");
        }

        this.cmpConfiguration.removeAlias(cmpAlias);
        this.globalConfigurationSession.saveConfiguration(ADMIN, this.cmpConfiguration);
    }
    
    @Override
    public String getRoleName() {
        return this.getClass().getSimpleName(); 
    }

    // client mode
    @Test
    public void test01CrmfHttpUnknowUser() throws Exception {
        log.trace(">test01CrmfHttpUnknowUser");
        // A name that does not exist
        byte[] nonce = CmpMessageHelper.createSenderNonce();
        byte[] transid = CmpMessageHelper.createSenderNonce();

        // USER_DN = USER_DN + ", serialNumber=01234567";
        PKIMessage req = genCertReq(ISSUER_DN, USER_DN, this.keys, this.cacert, nonce, transid, false, null, null, null, null, null, null);
        assertNotNull(req);
        CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
        int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DEROutputStream out = new DEROutputStream(bao);
        out.writeObject(req);
        byte[] ba = bao.toByteArray();

        byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, USER_DN, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        checkCmpFailMessage(resp, "Wrong username or password", 1, reqId, 7, PKIFailureInfo.incorrectData); // Expects a CertificateResponse (reject) message with error
                                                                                // FailInfo.INCORRECT_DATA
        log.trace("<test01CrmfHttpUnknowUser");
    }

    // client mode
    @Test
    public void test02CrmfHttpUnknowUserSignedMessage() throws Exception {
        // A name that does not exist
        byte[] nonce = CmpMessageHelper.createSenderNonce();
        byte[] transid = CmpMessageHelper.createSenderNonce();

        PKIMessage req = genCertReq(ISSUER_DN, USER_DN, this.keys, this.cacert, nonce, transid, false, null, null, null, null, null, null);
        assertNotNull(req);
        X509Certificate signCert = CertTools.genSelfCert("CN=CMP Sign Test", 3650, null, this.keys.getPrivate(), this.keys.getPublic(), "SHA256WithRSA", false);
        ArrayList<Certificate> signCertColl = new ArrayList<Certificate>();
        signCertColl.add(signCert);
        CmpMessageHelper.signPKIMessage(req, signCertColl, this.keys.getPrivate(), CMSSignedGenerator.DIGEST_SHA1, "BC");
        // PKIMessage req = protectPKIMessage(req1, false, "foo123", "mykeyid", 567);
        CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
        int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DEROutputStream out = new DEROutputStream(bao);
        out.writeObject(req);
        byte[] ba = bao.toByteArray();
        // Send request and receive response
        byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, USER_DN, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        checkCmpFailMessage(resp, "Wrong username or password", 1, reqId, 7, PKIFailureInfo.incorrectData); // Expects a CertificateResponse (reject) message with error
                                                                                // FailInfo.INCORRECT_DATA
    }

    @Test
    public void test03CrmfHttpOkUser() throws Exception {
        log.trace(">test03CrmfHttpOkUser");
        // Create a new good USER
        final X500Name userDN = createCmpUser("cmptest", "C=SE,O=PrimeKey,CN=cmptest", true);

        byte[] nonce = CmpMessageHelper.createSenderNonce();
        byte[] transid = CmpMessageHelper.createSenderNonce();

        PKIMessage req = genCertReq(ISSUER_DN, userDN, this.keys, this.cacert, nonce, transid, false, null, null, null, null, null, null);
        assertNotNull(req);
        CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
        int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DEROutputStream out = new DEROutputStream(bao);
        out.writeObject(req);
        byte[] ba = bao.toByteArray();
        // Send request and receive response
        byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        X509Certificate cert = checkCmpCertRepMessage(userDN, this.cacert, resp, reqId);
        String altNames = CertTools.getSubjectAlternativeName(cert);
        assertNull("AltNames was not null (" + altNames + ").", altNames);

        // Send a confirm message to the CA
        String hash = "foo123";
        PKIMessage confirm = genCertConfirm(userDN, this.cacert, nonce, transid, hash, reqId);
        assertNotNull(confirm);
        bao = new ByteArrayOutputStream();
        out = new DEROutputStream(bao);
        out.writeObject(confirm);
        ba = bao.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        checkCmpPKIConfirmMessage(userDN, this.cacert, resp);

        // Now revoke the bastard!
        PKIMessage rev = genRevReq(ISSUER_DN, userDN, cert.getSerialNumber(), this.cacert, nonce, transid, true, null, null);
        assertNotNull(rev);
        ByteArrayOutputStream baorev = new ByteArrayOutputStream();
        DEROutputStream outrev = new DEROutputStream(baorev);
        outrev.writeObject(rev);
        byte[] barev = baorev.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(barev, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        checkCmpFailMessage(resp, "PKI Message is not athenticated properly. No HMAC protection was found.", 23, reqId, 
                                PKIFailureInfo.badMessageCheck, PKIFailureInfo.incorrectData);
        log.trace("<test03CrmfHttpOkUser");
    }

    @Test
    public void test04BlueXCrmf() throws Exception {
        log.trace(">test04BlueXCrmf");
        byte[] resp = sendCmpHttp(bluexir, 200, cmpAlias);
        assertNotNull(resp);
        checkCmpPKIErrorMessage(resp, "C=NL,O=A.E.T. Europe B.V.,OU=Development,CN=Test CA 1", new X500Name(new RDN[0]), PKIFailureInfo.badPOP, null); // expecting a bad_pop
        log.trace("<test04BlueXCrmf");
    }

    @Test
    public void test05BadBytes() throws Exception {
        log.trace(">test05BadBytes");
        byte[] msg = bluexir;
        // Change some bytes to make the message bad
        msg[10] = 0;
        msg[15] = 0;
        msg[22] = 0;
        msg[56] = 0;
        msg[88] = 0;
        // Bad request will return HTTP 400 (bad request)
        byte[] resp = sendCmpHttp(msg, 400, cmpAlias);
        assertNull(resp);
        log.trace("<test05BadBytes");
    }

    /*
    public void test06TelefonicaGermany() throws Exception {
    	log.trace(">test05TelefonicaGermany");
    
    	HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] nonce = adapter.unmarshal("219F0452");			//CmpMessageHelper.createSenderNonce();
        byte[] transid = adapter.unmarshal("46E72888");			//CmpMessageHelper.createSenderNonce();
        
        byte[] resp = sendCmpHttp(telefonica, 200);
        //sender = USER_DN, recepient = ISSUER_DN. This does not sound right though!!!
        checkCmpResponseGeneral(resp, "C=cn,ST=sh,L=qc,O=wl,OU=lte,CN=enbca", "C=CN,O=Huawei,OU=Wireless Network Product Line,CN=21030533610000000012 eNodeB", cacert, nonce, transid, true, null);
        
    	/ *
        byte[] resp = sendCmpHttp(ba, 200);
        checkCmpResponseGeneral(resp, ISSUER_DN, USER_DN, cacert, nonce, transid, true, null);
        X509Certificate cert = checkCmpCertRepMessage(USER_DN, cacert, resp, reqId);
        String altNames = CertTools.getSubjectAlternativeName(cert);
        assertNull("AltNames was not null (" + altNames + ").", altNames);

        // Send a confirm message to the CA
        String hash = "foo123";
        PKIMessage confirm = genCertConfirm(USER_DN, cacert, nonce, transid, hash, reqId);
        assertNotNull(confirm);
        bao = new ByteArrayOutputStream();
        out = new DEROutputStream(bao);
        out.writeObject(confirm);
        ba = bao.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(ba, 200);
        checkCmpResponseGeneral(resp, ISSUER_DN, USER_DN, cacert, nonce, transid, false, null);
        checkCmpPKIConfirmMessage(USER_DN, cacert, resp);

        // Now revoke the bastard!
        PKIMessage rev = genRevReq(ISSUER_DN, USER_DN, cert.getSerialNumber(), cacert, nonce, transid, true);
        assertNotNull(rev);
        bao = new ByteArrayOutputStream();
        out = new DEROutputStream(bao);
        out.writeObject(rev);
        ba = bao.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(ba, 200);
        checkCmpResponseGeneral(resp, ISSUER_DN, USER_DN, cacert, nonce, transid, false, null);
        checkCmpFailMessage(resp, "No PKI protection to verify.", 23, reqId, 1);
        * /
    	log.trace("<test05TelefonicaGermany");
    }
    */

    @Test
    public void test07SignedConfirmationMessage() throws Exception {
        log.trace(">test07SignedConfirmationMessage()");
        CmpConfirmResponseMessage cmpConfRes = new CmpConfirmResponseMessage();
        cmpConfRes.setSignKeyInfo(this.testx509ca.getCertificateChain(), this.keys.getPrivate(), null);
        cmpConfRes.setSender(new GeneralName(USER_DN));
        cmpConfRes.setRecipient(new GeneralName(new X500Name("CN=cmpRecipient, O=TEST")));
        cmpConfRes.setSenderNonce("DAxFSkJDQSBTYW");
        cmpConfRes.setRecipientNonce("DAxFSkJDQSBTYY");
        cmpConfRes.setTransactionId("DAxFS");
        cmpConfRes.create();
        byte[] resp = cmpConfRes.getResponseMessage();
        PKIMessage msg = PKIMessage.getInstance(ASN1Primitive.fromByteArray(resp));
        boolean veriStatus = CmpMessageHelper.verifyCertBasedPKIProtection(msg, this.keys.getPublic());
        assertTrue("Verification failed.", veriStatus);
        log.trace("<test07SignedConfirmationMessage()");
    }

    @Test
    public void test08SubjectDNSerialnumber() throws Exception {
        log.trace(">test08SubjectDNSerialnumber");
        // Create a new good USER
        String cmpsntestUsername = "cmpsntest";
        String cmpsntest2Username = "cmpsntest2";
        final X500Name userDN1 = createCmpUser(cmpsntestUsername, "C=SE,SN=12234567,CN=cmpsntest", true);
        
        try {
        byte[] nonce = CmpMessageHelper.createSenderNonce();
        byte[] transid = CmpMessageHelper.createSenderNonce();
        
        PKIMessage req = genCertReq(ISSUER_DN, userDN1, this.keys, this.cacert, nonce, transid, false, null, null, null, null, null, null);
        assertNotNull(req);
        CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
        int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        DEROutputStream out = new DEROutputStream(bao);
        out.writeObject(req);
        byte[] ba = bao.toByteArray();
        // Send request and receive response
        byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN1, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        X509Certificate cert = checkCmpCertRepMessage(userDN1, this.cacert, resp, reqId);
        
        // Now revoke the certificate!
        PKIMessage rev = genRevReq(ISSUER_DN, userDN1, cert.getSerialNumber(), this.cacert, nonce, transid, true, null, null);
        assertNotNull(rev);        
        rev = protectPKIMessage(rev, false, "foo123", 567);
        assertNotNull(rev);
        ByteArrayOutputStream baorev = new ByteArrayOutputStream();
        DEROutputStream outrev = new DEROutputStream(baorev);
        outrev.writeObject(rev);
        byte[] barev = baorev.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(barev, 200,cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN1, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        int revStatus = checkRevokeStatus(ISSUER_DN, CertTools.getSerialNumber(cert));
        assertNotEquals("Revocation request failed to revoke the certificate", RevokedCertInfo.NOT_REVOKED, revStatus);

        // Create another USER with the subjectDN serialnumber spelled "SERIALNUMBER" instead of "SN"
        KeyPair keys2 = KeyTools.genKeys("512", AlgorithmConstants.KEYALGORITHM_RSA);
        
        final X500Name userDN2 = createCmpUser(cmpsntest2Username, "C=SE,SERIALNUMBER=123456789,CN=cmpsntest2", true);
        req = genCertReq(ISSUER_DN, userDN2, keys2, this.cacert, nonce, transid, false, null, null, null, null, null, null);
        assertNotNull(req);
        ir = (CertReqMessages) req.getBody().getContent();
        reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
        bao = new ByteArrayOutputStream();
        out = new DEROutputStream(bao);
        out.writeObject(req);
        ba = bao.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(ba, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN2, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        cert = checkCmpCertRepMessage(userDN2, this.cacert, resp, reqId);
        
        // Now revoke this certificate too
        rev = genRevReq(ISSUER_DN, userDN2, cert.getSerialNumber(), this.cacert, nonce, transid, true, null, null);
        assertNotNull(rev);
        rev = protectPKIMessage(rev, false, "foo123", 567);
        assertNotNull(rev);
        baorev = new ByteArrayOutputStream();
        outrev = new DEROutputStream(baorev);
        outrev.writeObject(rev);
        barev = baorev.toByteArray();
        // Send request and receive response
        resp = sendCmpHttp(barev, 200, cmpAlias);
        checkCmpResponseGeneral(resp, ISSUER_DN, userDN2, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
        revStatus = checkRevokeStatus(ISSUER_DN, CertTools.getSerialNumber(cert));
        assertNotEquals("Revocation request failed to revoke the certificate", RevokedCertInfo.NOT_REVOKED, revStatus);
        
        log.trace("<test08SubjectDNSerialnumber");
        } finally {
            this.endEntityManagementSession.deleteUser(ADMIN, cmpsntestUsername);
            this.endEntityManagementSession.deleteUser(ADMIN, cmpsntest2Username);
        }
    }


    @Test
    public void test09KeyIdTest() {
        log.trace(">test09KeyIdTest()");
        
        DEROctetString octs = new DEROctetString("foo123".getBytes());
        String keyid = CmpMessageHelper.getStringFromOctets(octs);
        assertEquals("foo123", keyid);
            
        PKIHeaderBuilder headerbuilder = new PKIHeaderBuilder(2, new GeneralName(new X500Name("CN=Sender")), new GeneralName(new X500Name("CN=Recipient")));
        headerbuilder.setSenderKID(new DEROctetString("foo123".getBytes()));
        PKIHeader header = headerbuilder.build();
        keyid = CmpMessageHelper.getStringFromOctets(header.getSenderKID());
        assertEquals("foo123", keyid);
        
        log.trace("<test09KeyIdTest()");
    }

    @Test
    public void  test10EscapedCharsInDN() throws Exception {
        log.trace(">test10EscapedCharsInDN");

        this.cmpConfiguration.setExtractUsernameComponent(cmpAlias, "DN");
        this.globalConfigurationSession.saveConfiguration(ADMIN, this.cmpConfiguration);
        

        byte[] nonce = CmpMessageHelper.createSenderNonce();
        byte[] transid = CmpMessageHelper.createSenderNonce();
        
        // --------------- Send a CRMF request with the whole DN as username with escapable characters --------------- //
        final String sRequestName = "CN=another\0nullguy%00<do>";
        // Create a new good USER
        final X500Name requestName = createCmpUser(sRequestName, sRequestName, false);

        try {
            PKIMessage req = genCertReq(ISSUER_DN, requestName, this.keys, this.cacert, nonce, transid, false, null, null, null, null, null, null);
            assertNotNull(req);
            CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
            int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DEROutputStream out = new DEROutputStream(bao);
            out.writeObject(req);
            byte[] ba = bao.toByteArray();
            // Send request and receive response
            byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
            checkCmpResponseGeneral(resp, ISSUER_DN, requestName, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
            X509Certificate cert = checkCmpCertRepMessage(new X500Name(StringTools.strip(sRequestName)), this.cacert, resp, reqId);
            assertNotNull(cert);

            // Now revoke the bastard!
            PKIMessage rev = genRevReq(ISSUER_DN, requestName, cert.getSerialNumber(), this.cacert, nonce, transid, true, null, null);
            assertNotNull(rev);
            rev = protectPKIMessage(rev, false, "foo123", 567);
            ByteArrayOutputStream baorev = new ByteArrayOutputStream();
            DEROutputStream outrev = new DEROutputStream(baorev);
            outrev.writeObject(rev);
            byte[] barev = baorev.toByteArray();
            // Send request and receive response
            resp = sendCmpHttp(barev, 200, cmpAlias);
            checkCmpResponseGeneral(resp, ISSUER_DN, requestName, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
            int revStatus = checkRevokeStatus(ISSUER_DN, CertTools.getSerialNumber(cert));
            assertNotEquals("Revocation request failed to revoke the certificate", RevokedCertInfo.NOT_REVOKED, revStatus);
        } finally {
            String escapedName = StringTools.stripUsername(sRequestName);
            try {
                this.endEntityManagementSession.deleteUser(ADMIN, escapedName);
            } catch (NotFoundException e) {
                // A test probably failed before creating the entity
                log.debug("Failed to delete USER: " + escapedName);
            }
        }
        
        
        // --------------- Send a CRMF request with a username with escapable characters --------------- //
        final String username = "another\0nullguy%00";
        final String sDN = "CN=" + username + ", C=SE, O=hejsan";
        KeyPair key2 = KeyTools.genKeys("512", AlgorithmConstants.KEYALGORITHM_RSA);
        
        // Create a new good USER
        final X500Name dn = createCmpUser(username, sDN, false);

        try {
        
            PKIMessage req = genCertReq(ISSUER_DN, dn, key2, this.cacert, nonce, transid, false, null, null, null, null, null, null);
            assertNotNull(req);
            CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
            int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DEROutputStream out = new DEROutputStream(bao);
            out.writeObject(req);
            byte[] ba = bao.toByteArray();
            // Send request and receive response
            byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
            checkCmpResponseGeneral(resp, ISSUER_DN, dn, this.cacert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
            X509Certificate cert = checkCmpCertRepMessage(dn, this.cacert, resp, reqId);
            assertNotNull(cert);

            // Now revoke the bastard!
            PKIMessage rev = genRevReq(ISSUER_DN, dn, cert.getSerialNumber(), this.cacert, nonce, transid, true, null, null);
            assertNotNull(rev);
            rev = protectPKIMessage(rev, false, "foo123", 567);
            ByteArrayOutputStream baorev = new ByteArrayOutputStream();
            DEROutputStream outrev = new DEROutputStream(baorev);
            outrev.writeObject(rev);
            byte[] barev = baorev.toByteArray();
            // Send request and receive response
            resp = sendCmpHttp(barev, 200, cmpAlias);
            checkCmpResponseGeneral(resp, ISSUER_DN, dn, this.cacert, nonce, transid, false, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
            int revStatus = checkRevokeStatus(ISSUER_DN, CertTools.getSerialNumber(cert));
            assertNotEquals("Revocation request failed to revoke the certificate", RevokedCertInfo.NOT_REVOKED, revStatus);
        
        } finally {
            String escapedName = StringTools.strip(username);
            try {
                this.endEntityManagementSession.deleteUser(ADMIN, escapedName);
            } catch (NotFoundException e) {
                // A test probably failed before creating the entity
                log.debug("Failed to delete USER: " + escapedName);
            }
        }
        
        log.trace("<test10EscapedCharsInDN");
    }
    

    @Test
    public void  test11IncludingCertChainInSignedCMPResponse() throws Exception {
        
        //---------- Create SubCA signed by testx509ca (rootCA) ------------- //
        String subcaDN = "CN=SubTestCA";
        int subcaID = subcaDN.hashCode();
        int cryptoTokenId = CryptoTokenTestUtils.createCryptoTokenForCA(ADMIN, null, true, false, subcaDN, "1024");
        final String username = "cmptest";
        try {
            final CAToken catoken = CaTestUtils.createCaToken(cryptoTokenId, AlgorithmConstants.SIGALG_SHA256_WITH_RSA, AlgorithmConstants.SIGALG_SHA256_WITH_RSA);
            final List<ExtendedCAServiceInfo> extendedCaServices = new ArrayList<ExtendedCAServiceInfo>(2);
            extendedCaServices.add(new KeyRecoveryCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE));
            String caname = CertTools.getPartFromDN(subcaDN, "CN");
            boolean ldapOrder = !CertTools.isDNReversed(subcaDN);
            X509CAInfo cainfo = new X509CAInfo(subcaDN, caname, CAConstants.CA_ACTIVE, CertificateProfileConstants.CERTPROFILE_FIXED_SUBCA,
                    3650, this.caid, this.testx509ca.getCertificateChain(), catoken);
            cainfo.setDescription("JUnit RSA SubCA");
            cainfo.setExtendedCAServiceInfos(extendedCaServices);
            cainfo.setUseLdapDnOrder(ldapOrder);
            cainfo.setCmpRaAuthSecret("foo123");

            CAAdminSessionRemote caAdminSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CAAdminSessionRemote.class);
            caAdminSession.createCA(ADMIN, cainfo);
            assertTrue(this.caSession.existsCa(subcaID));
            cainfo = (X509CAInfo) this.caSession.getCAInfo(ADMIN, subcaID);
            X509Certificate subcaCert = (X509Certificate) cainfo.getCertificateChain().iterator().next();

            // --------- Create a user ----------------- //
            boolean userExists = false;
            final X500Name userDN = new X500Name("C=SE,O=PrimeKey,CN=cmptest");
            EndEntityInformation user = new EndEntityInformation("cmptest", userDN.toString(), subcaID,
                    null, "cmptest@primekey.se", new EndEntityType(EndEntityTypes.ENDUSER),
//                    SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, 
                    this.eepDnOverrideId, this.cpDnOverrideId,
                    SecConst.TOKEN_SOFT_PEM, 0, null);
            user.setPassword("foo123");
            try {
                this.endEntityManagementSession.addUser(ADMIN, user, true); 
                log.debug("created user: cmptest, foo123, " + userDN);
            } catch (Exception e) {
                userExists = true;
            }

            if (userExists) {
                log.debug("User cmptest already exists.");
                this.endEntityManagementSession.changeUser(ADMIN, user, true);
                this.endEntityManagementSession.setUserStatus(ADMIN, "cmptest", EndEntityConstants.STATUS_NEW);
                log.debug("Reset status to NEW");
            }


            assertTrue(this.endEntityManagementSession.existsUser("cmptest"));
            EndEntityAccessSessionRemote eeAccessSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityAccessSessionRemote.class);
            EndEntityInformation ee = eeAccessSession.findUser(ADMIN, "cmptest");
            assertEquals(subcaID, ee.getCAId());


            // -------- generate and send a CMP request -------------- //
            byte[] nonce = CmpMessageHelper.createSenderNonce();
            byte[] transid = CmpMessageHelper.createSenderNonce();

            PKIMessage req = genCertReq(subcaDN, userDN, this.keys, subcaCert, nonce, transid, false, null, null, null, null, null, null);
            assertNotNull(req);
            CertReqMessages ir = (CertReqMessages) req.getBody().getContent();
            int reqId = ir.toCertReqMsgArray()[0].getCertReq().getCertReqId().getValue().intValue();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            DEROutputStream out = new DEROutputStream(bao);
            out.writeObject(req);
            byte[] ba = bao.toByteArray();
            // Send request and receive response
            byte[] resp = sendCmpHttp(ba, 200, cmpAlias);
            checkCmpResponseGeneral(resp, subcaDN, userDN, subcaCert, nonce, transid, true, null, PKCSObjectIdentifiers.sha1WithRSAEncryption.getId());
            final X509Certificate cert = checkCmpCertRepMessage(userDN, subcaCert, resp, reqId);
            assertNotNull(cert);

            // ------- Check that the entire certificate chain is in the extraCerts field in the response
            PKIMessage respMsg = null;
            ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(resp));
            try {
                respMsg = PKIMessage.getInstance(asn1InputStream.readObject());
            } finally {
                asn1InputStream.close();
            }
            assertNotNull(respMsg);

            CMPCertificate[] certChain = respMsg.getExtraCerts();
            assertEquals(2, certChain.length);
            assertEquals(subcaDN, certChain[0].getX509v3PKCert().getSubject().toString());
            assertEquals(ISSUER_DN, certChain[1].getX509v3PKCert().getSubject().toString());
        } finally {
            try {
                this.endEntityManagementSession.deleteUser(ADMIN, username);
            } catch (NotFoundException e) {
                // A test probably failed before creating the entity
                log.debug("Failed to delete user: " + username);
            }
            CryptoTokenTestUtils.removeCryptoToken(null, cryptoTokenId);
            // Remove CA certificate of CA that we will remove
            Collection<Certificate> certs = this.caSession.getCAInfo(ADMIN, subcaID).getCertificateChain();
            this.internalCertStoreSession.removeCertificate(certs.iterator().next());
            // Remove the CA itself
            this.caSession.removeCA(ADMIN, subcaID);
        }
    }

    
    
    
    private X500Name createCmpUser(String username, String dn, boolean useDnOverride) throws AuthorizationDeniedException, UserDoesntFullfillEndEntityProfile, WaitingForApprovalException,
            EjbcaException, FinderException, CADoesntExistsException {
        // Make USER that we know...
        boolean userExists = false;
        final int cpID;
        final int eepID;
        final X500Name userDN;
        if ( useDnOverride ) {
            cpID = this.cpDnOverrideId;
            eepID = this.eepDnOverrideId;
            userDN = new X500Name(dn);
        } else {
            cpID = CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER;
            eepID = SecConst.EMPTY_ENDENTITYPROFILE;
            userDN = new X500Name(StringTools.strip(CertTools.stringToBCDNString(dn)));
        }
        final EndEntityInformation user = new EndEntityInformation(
                username, dn, this.caid, null, username + "@primekey.se",
                new EndEntityType(EndEntityTypes.ENDUSER),
                eepID, cpID, SecConst.TOKEN_SOFT_PEM, 0, null );
        user.setPassword("foo123");
        log.debug("Trying to add/edit USER: " + user.getUsername() + ", foo123, " + userDN);
        try {
            this.endEntityManagementSession.addUser(ADMIN, user, true); 
        } catch (Exception e) {
            userExists = true;
        }

        if (userExists) {
            log.debug("USER already exists: " + user.getUsername() + ", foo123, " + userDN);
            this.endEntityManagementSession.changeUser(ADMIN, user, true);
            this.endEntityManagementSession.setUserStatus(ADMIN, user.getUsername(), EndEntityConstants.STATUS_NEW);
            log.debug("Reset status to NEW");
        }
        return userDN;
    }

    static byte[] bluexir = Base64.decode(("MIICIjCB1AIBAqQCMACkVjBUMQswCQYDVQQGEwJOTDEbMBkGA1UEChMSQS5FLlQu"
            + "IEV1cm9wZSBCLlYuMRQwEgYDVQQLEwtEZXZlbG9wbWVudDESMBAGA1UEAxMJVGVz" + "dCBDQSAxoT4wPAYJKoZIhvZ9B0INMC8EEAK/H7Do+55N724Kdvxm7NcwCQYFKw4D"
            + "AhoFAAICA+gwDAYIKwYBBQUIAQIFAKILBAlzc2xjbGllbnSkEgQQpFpBsonfhnW8" + "ia1otGchraUSBBAyzd3nkKAzcJqGFrDw0jkYoIIBLjCCASowggEmMIIBIAIBADCC"
            + "ARmkJqARGA8yMDA2MDkxOTE2MTEyNlqhERgPMjAwOTA2MTUxNjExMjZapR0wGzEZ" + "MBcGA1UEAwwQU29tZSBDb21tb24gTmFtZaaBoDANBgkqhkiG9w0BAQEFAAOBjgAw"
            + "gYoCgYEAuBgTGPgXrS3AIPN6iXO6LNf5GzAcb/WZhvebXMdxdrMo9+5hw/Le5St/" + "Sz4J93rxU95b2LMuHTg8U6njxC2lZarNExZTdEwnI37X6ep7lq1purq80zD9bFXj"
            + "ougRD5MHfhDUAQC+btOgEXkanoAo8St3cbtHoYUacAXN2Zs/RVcCBAABAAGpLTAr" + "BgNVHREEJDAioCAGCisGAQQBgjcUAgOgEgwQdXBuQGFldGV1cm9wZS5ubIAAoBcD"
            + "FQAy/vSoNUevcdUxXkCQx3fvxkjh6A==").getBytes());

    /*
     *	header:
     *		pvno: cmp2000 (cmp.pvno = 2)
     *		sender: 4	(cmp.sender = 4)
     *			directoryName: rdnSequence (0)		(x509ce.directoryName = 0)
     *				rdnSequence: 4 items (id-at-commonName=21030533610000000012 eNodeB,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *					RDNSequence item: 1 item (id-at-countryName=CN)					(x509if.RDNSequence_item = 1)
     *						RelativeDistinguishedName item (id-at-countryName=CN)		(x509if.RelativeDistinguishedName_item = 1)
     *							Id: 2.5.4.6 (id-at-countryName)							(x509if.id = 2.5.4.6)
     *							CountryName: CN											(x509sat.CountryName = CN)
     *					RDNSequence item: 1 item (id-at-organizationName=Huawei)
     *					RDNSequence item: 1 item (id-at-organizationalUnitName=Wireless Network Product Line)
     *					RDNSequence item: 1 item (id-at-commonName=21030533610000000012 eNodeB)
     *		recipient: 4
     *			directoryName: rdnSequence (0)
     *				rdnSequence: 6 items (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					RDNSequence item: 1 item (id-at-countryName=cn)
     *					RDNSequence item: 1 item (id-at-stateOrProvinceName=sh)
     *					RDNSequence item: 1 item (id-at-localityName=qc)
     *					RDNSequence item: 1 item (id-at-organizationName=wl)
     *					RDNSequence item: 1 item (id-at-organizationalUnitName=lte)
     *					RDNSequence item: 1 item (id-at-commonName=enbca)
     *		protectionAlg (shaWithRSAEncryption)
     *			Algorithm Id: 1.2.840.113549.1.1.5 (shaWithRSAEncryption)
     *		transactionID: 46E72888
     *		senderNonce: 219F0452
     *		recipNonce: 00000000
     *	body: ir (0)
     *		ir: 1 item
     *			CertReqMsg
     *				certReq
     *					certReqId: 355
     *					certTemplate
     *						version: v3 (2)
     *						validity
     *							notBefore: utcTime (0)
     *								utcTime: 10-06-01 09:44:01 (UTC)
     *							notAfter: utcTime (0)
     *								utcTime: 11-06-01 09:44:01 (UTC)
     *						subject: 0
     *							rdnSequence: 1 item (id-at-commonName=21030533610000000012 eNodeB)
     *								RDNSequence item: 1 item (id-at-commonName=21030533610000000012 eNodeB)
     *									RelativeDistinguishedName item (id-at-commonName=21030533610000000012 eNodeB)
     *										Id: 2.5.4.3 (id-at-commonName)
     *										DirectoryString: uTF8String (4)
     *											uTF8String: 21030533610000000012 eNodeB
     *						publicKey
     *							algorithm (rsaEncryption)
     *								Algorithm Id: 1.2.840.113549.1.1.1 (rsaEncryption)
     *							Padding: 0
     *							subjectPublicKey: 3082010A02820101009C2BCD07CBB0CF2B8B75062668D64F...
     *						extensions: 2 items
     *							Extension
     *								Id: 2.5.29.15 (id-ce-keyUsage)
     *								critical: True
     *								Padding: 3
     *								KeyUsage: B8 (digitalSignature, keyEncipherment, dataEncipherment, keyAgreement)
     *									1... .... = digitalSignature: True
     *									.0.. .... = contentCommitment: False
     *									..1. .... = keyEncipherment: True
     *									...1 .... = dataEncipherment: True
     *									.... 1... = keyAgreement: True
     *									.... .0.. = keyCertSign: False
     *									.... ..0. = cRLSign: False
     *									.... ...0 = encipherOnly: False
     *									0... .... = decipherOnly: False
     *							Extension
     *								Id: 2.5.29.17 (id-ce-subjectAltName)
     *								critical: True
     *								GeneralNames: 1 item
     *									GeneralName: dNSName (2)
     *										dNSName: 21030533610000000012.huawei.com
     *				popo: signature (1)
     *					signature
     *						algorithmIdentifier (shaWithRSAEncryption)
     *							Algorithm Id: 1.2.840.113549.1.1.5 (shaWithRSAEncryption)
     *						Padding: 0
     *						signature: 403F2C7C4A1C777D3F09132FBBAC3FCA058CD4EE1F461F24...
     *		Padding: 0
     *		protection: 73FEA50585570F1B3CD16E3A744546251D0C206FC67B2554...
     *		extraCerts: 3 items
     *			CMPCertificate: x509v3PKCert (0)
     *				signedCertificate
     *					version: v3 (2)
     *					serialNumber : 0x00bad55b3947cb876dc391f7798438d2a5
     *					signature (shaWithRSAEncryption) : 
     *						Algorithm Id: 1.2.840.113549.1.1.5 (shaWithRSAEncryption)
     *					issuer: rdnSequence (0)
     *						rdnSequence: 4 items (id-at-commonName=Huawei Wireless Network Product CA,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *					validity
     *						notBefore: utcTime (0)
     *							utcTime: 10-11-12 07:39:38 (UTC)
     *						notAfter: utcTime (0)
     *							utcTime: 34-10-17 09:00:35 (UTC)
     *					subject: rdnSequence (0)
     *						rdnSequence: 4 items (id-at-commonName=21030533610000000012 eNodeB,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *					subjectPublicKeyInfo
     *						algorithm (rsaEncryption)
     *							Algorithm Id: 1.2.840.113549.1.1.1 (rsaEncryption)
     *						Padding: 0
     *						subjectPublicKey: 30818902818100BE8880B56877C44F300EAB825C198B8FF3...
     *					extensions: 2 items
     *						Extension (id-ce-keyUsage)
     *							Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *							critical: True
     *							Padding: 0
     *							KeyUsage: B8 (digitalSignature, keyEncipherment, dataEncipherment, keyAgreement)
     *						Extension Id: 2.5.29.17 (id-ce-subjectAltName)
     *							GeneralNames: 1 item
     *								GeneralName: dNSName (2)
     *									dNSName: 21030533610000000012.Huawei.com
     *			CMPCertificate: x509v3PKCert (0)
     *				x509v3PKCert (id-at-commonName=Huawei Wireless Network Product CA,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *					signedCertificate
     *						version: v3 (2)
     *						serialNumber : 0x00b2c83453e95b7df146f96729bdd7172c
     *						signature (shaWithRSAEncryption)
     *						issuer: rdnSequence (0)
     *							rdnSequence: 3 items (id-at-commonName=Huawei Equipment CA,id-at-organizationName=Huawei,id-at-countryName=CN)
     *						validity
     *							notBefore: utcTime (0)
     *								utcTime: 09-10-19 09:30:34 (UTC)
     *							notAfter: utcTime (0)
     *								utcTime: 34-10-18 09:00:35 (UTC)
     *						subject: rdnSequence (0)
     *							rdnSequence: 4 items (id-at-commonName=Huawei Wireless Network Product CA,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *						subjectPublicKeyInfo
     *							algorithm (rsaEncryption)
     *							Padding: 0
     *							subjectPublicKey: 3082010A0282010100C137F5D3877167EFA1CEDD31D27FAE...
     *						extensions: 4 items
     *							Extension (id-ce-basicConstraints)
     *								Extension Id: 2.5.29.19 (id-ce-basicConstraints)
     *								BasicConstraintsSyntax
     *									cA: True
     *							Extension (id-ce-keyUsage)
     *								Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *									critical: True
     *									Padding: 1
     *									KeyUsage: 06 (keyCertSign, cRLSign)
     *							Extension (id-ce-subjectKeyIdentifier)
     *								Extension Id: 2.5.29.14 (id-ce-subjectKeyIdentifier)
     *									SubjectKeyIdentifier: 5E7017DC6FA40748033787FE3DB4C720D636B8D0
     *							Extension (id-ce-authorityKeyIdentifier)
     *								Extension Id: 2.5.29.35 (id-ce-authorityKeyIdentifier)
     *								AuthorityKeyIdentifier
     *									keyIdentifier: 2AF810592780351FA77CBA3B9F2AE44AAA9B92EA
     *					algorithmIdentifier (shaWithRSAEncryption)
     *					Padding: 0
     *					encrypted: 931FC67E865E1969E22B29A5C578A0EBB79E5A0AE29EC888...
     *			CMPCertificate: x509v3PKCert (0)
     *				x509v3PKCert (id-at-commonName=Huawei Equipment CA,id-at-organizationName=Huawei,id-at-countryName=CN)
     *					signedCertificate
     *						version: v3 (2)
     *						serialNumber : 0x00f2ff51cc6584f1980824d984b3cdbd5b
     *						signature (shaWithRSAEncryption)
     *						issuer: rdnSequence (0)
     *							rdnSequence: 3 items (id-at-commonName=Huawei Equipment CA,id-at-organizationName=Huawei,id-at-countryName=CN)
     *						validity
     *							notBefore: utcTime (0)
     *								utcTime: 09-10-19 09:00:28 (UTC)
     *							notAfter: utcTime (0)
     *								utcTime: 34-10-19 09:00:00 (UTC)
     *						subject: rdnSequence (0)
     *							rdnSequence: 3 items (id-at-commonName=Huawei Equipment CA,id-at-organizationName=Huawei,id-at-countryName=CN)
     *						subjectPublicKeyInfo
     *							algorithm (rsaEncryption)
     *							Padding: 0
     *							subjectPublicKey: 3082020A0282020100A28984270BF329F686E60275E6BBF3...
     *						extensions: 4 items
     *							Extension (id-ce-keyUsage)
     *								Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *								critical: True
     *								Padding: 1
     *								KeyUsage: 86 (digitalSignature, keyCertSign, cRLSign)
     *							Extension (id-ce-basicConstraints)
     *								Extension Id: 2.5.29.19 (id-ce-basicConstraints)
     *								BasicConstraintsSyntax
     *									cA: True
     *							Extension (id-ce-subjectKeyIdentifier)
     *								Extension Id: 2.5.29.14 (id-ce-subjectKeyIdentifier)
     *								SubjectKeyIdentifier: 2AF810592780351FA77CBA3B9F2AE44AAA9B92EA
     *							Extension (id-ce-authorityKeyIdentifier)
     *								Extension Id: 2.5.29.35 (id-ce-authorityKeyIdentifier)
     *								AuthorityKeyIdentifier
     *									keyIdentifier: 2AF810592780351FA77CBA3B9F2AE44AAA9B92EA
     *					algorithmIdentifier (shaWithRSAEncryption)
     *					Padding: 0
     *					encrypted: 000B6246A8239D21F35786BBE6E6E96E8E7D7C17C7679C87...
     */
    static byte[] telefonica = Base64.decode(("MIIRmTCB8gIBAqRuMGwxCzAJBgNVBAYTAkNOMQ8wDQYDVQQKEwZIdWF3ZWkxJjAkBgNVBAsTHVdp"
            + "cmVsZXNzIE5ldHdvcmsgUHJvZHVjdCBMaW5lMSQwIgYDVQQDExsyMTAzMDUzMzYxMDAwMDAwMDAx"
            + "MiBlTm9kZUKkVDBSMQswCQYDVQQGEwJjbjELMAkGA1UECBMCc2gxCzAJBgNVBAcTAnFjMQswCQYD"
            + "VQQKEwJ3bDEMMAoGA1UECxMDbHRlMQ4wDAYDVQQDEwVlbmJjYaEPMA0GCSqGSIb3DQEBBQUApAYE"
            + "BEbnKIilBgQEIZ8EUqYGBAQAAAAAoIIC5DCCAuAwggLcMIIBwAICAWMwggG4gAECpCKgDxcNMTAw"
            + "NjAxMDk0NDAxWqEPFw0xMTA2MDEwOTQ0MDFapSgwJjEkMCIGA1UEAwwbMjEwMzA1MzM2MTAwMDAw"
            + "MDAwMTIgZU5vZGVCpoIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnCvNB8uwzyuLdQYm"
            + "aNZPP3jAZ0DL+9iPzJPaHUdQi2qG5tkoYy6UcH/WlJM90QIgr+XHK6rLCLWnk07APf/F9UDxhCpn"
            + "9BWM51c4MwSDnoSvFIdqOwsTSAirvkUAscF3OeW34RrXZRCmsl5jSND4MuRyUsDQcty1U/bj1U4g"
            + "lQdC+RwjwBYFK2K580ugEuz/x4nUtfqyjv7FFPY1ct2e5dQ/9Pbg/tq06oxMLuWO53IVRZ0WwACQ"
            + "bUIcr0bdlfwm7WqkHJEU51SdEDisfS/SyiK5NYfjEa2D/ZiGLREUgUx5uDc4NNjdHOycQ/0L1i9z"
            + "aOoyKbadUZFITdcglHaS4wIDAQABqT8wDgYDVR0PAQH/BAQDAgO4MC0GA1UdEQEB/wQjMCGCHzIx"
            + "MDMwNTMzNjEwMDAwMDAwMDEyLmh1YXdlaS5jb22hggEUMA0GCSqGSIb3DQEBBQUAA4IBAQBAPyx8"
            + "Shx3fT8JEy+7rD/KBYzU7h9GHyQ9fvdvUmVuqCvIVncbXwEDk+vInvkiCoBRgJxI2tmiwguJT4mQ"
            + "yIq4TBdunabLqEbL7Me36cYQH3mY68v4YzAnHYcM7eAcdxXDivxFuKwSxQ2yoVrncaPb8/tHmQdx"
            + "XOzi0MmkksFe3IR25qh6G9Jz+TRmGWtTuzEuF87oyUyUb8boCLeMJ5FUKidavI/fmqSKa+iX0vVW"
            + "T069pXCdtWdOZA4dc6ya7AEIifNUTLon03a/rtWXat+J4qnH1u2u2UgmItoiXjcur2tEGnPiGpxl"
            + "GiP+qbWQBzNM0GRIO7ldjbMztsLYSGd2oIGEA4GBAHP+pQWFVw8bPNFuOnRFRiUdDCBvxnslVOHD"
            + "2e5864lisPtoeSUXsLM/6Dqfa8Q8WDiKRht4t7X5QEr8aYv/Q7g4g9Q7MBl3UgV2xt44XS2c1ZXA"
            + "cbVvE6KzTFKlq5LtVsVsTFfnO1OiGrdwXzxeTNu94QUcLg7MkvhT4AON/QzwoYINMTCCDS0wggMk"
            + "MIICDKADAgECAhEAutVbOUfLh23Dkfd5hDjSpTANBgkqhkiG9w0BAQUFADBzMQswCQYDVQQGEwJD"
            + "TjEPMA0GA1UEChMGSHVhd2VpMSYwJAYDVQQLEx1XaXJlbGVzcyBOZXR3b3JrIFByb2R1Y3QgTGlu"
            + "ZTErMCkGA1UEAxMiSHVhd2VpIFdpcmVsZXNzIE5ldHdvcmsgUHJvZHVjdCBDQTAeFw0xMDExMTIw"
            + "NzM5MzhaFw0zNDEwMTcwOTAwMzVaMGwxCzAJBgNVBAYTAkNOMQ8wDQYDVQQKEwZIdWF3ZWkxJjAk"
            + "BgNVBAsTHVdpcmVsZXNzIE5ldHdvcmsgUHJvZHVjdCBMaW5lMSQwIgYDVQQDExsyMTAzMDUzMzYx"
            + "MDAwMDAwMDAxMiBlTm9kZUIwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAL6IgLVod8RPMA6r"
            + "glwZi4/zrgSSh1+04JLuB7Xbm3dGFmK8BoqUMqMBOtaE5x+apY6x8ZfJYLpLZQ1GfnsEEwJtUIh3"
            + "9zsGXKW8m5nCsXK6z0j7/t1a9ZdD1/4cAVN5bap6HLxC2bLKIsiiXsMr/6bvq5hCmoHLzHEG6TAP"
            + "I6qHAgMBAAGjPjA8MA4GA1UdDwEB/wQEAwIAuDAqBgNVHREEIzAhgh8yMTAzMDUzMzYxMDAwMDAw"
            + "MDAxMi5IdWF3ZWkuY29tMA0GCSqGSIb3DQEBBQUAA4IBAQB0hZ1CqMQLWzyYmxB/2X5s8BWX32zM"
            + "dk5M0X9xe7k4TuNyCCcv7GjYEVdda95VS0GPkYs8tUxaVPb2SQv7W5uNXy7sz6hr56xPJlbpkt01"
            + "yJYknlXFK4L+nEG7tszuSdu+1Q2gcO9OUOrkrm4I9Nx7KNhJuYtXjAtrs8DSmGITKtY1r3d63CAo"
            + "JuOGeBirRmMeiXCYlEZjLYrd14b0cp51FuKcj883DESTjHysc7Z3fHujqY3ZRhwaUqItYyGYSufN"
            + "wPmbmzZ5vBH813qekKeTh+4nK3pUTwSx4exXhIOqpWHyx9WGsLrDJ38EC8Mw1DJh4zMyfKGuGsKH"
            + "CukbJWkTMIIEmjCCAoKgAwIBAgIRALLINFPpW33xRvlnKb3XFywwDQYJKoZIhvcNAQEFBQAwPDEL"
            + "MAkGA1UEBhMCQ04xDzANBgNVBAoTBkh1YXdlaTEcMBoGA1UEAxMTSHVhd2VpIEVxdWlwbWVudCBD"
            + "QTAeFw0wOTEwMTkwOTMwMzRaFw0zNDEwMTgwOTAwMzVaMHMxCzAJBgNVBAYTAkNOMQ8wDQYDVQQK"
            + "EwZIdWF3ZWkxJjAkBgNVBAsTHVdpcmVsZXNzIE5ldHdvcmsgUHJvZHVjdCBMaW5lMSswKQYDVQQD"
            + "EyJIdWF3ZWkgV2lyZWxlc3MgTmV0d29yayBQcm9kdWN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOC"
            + "AQ8AMIIBCgKCAQEAwTf104dxZ++hzt0x0n+uRZahqaQYMO9qr7trvKo8XE+1mrxGbfbR3Yc8ArOJ"
            + "FQvfxq+ylI9L7qyunHEHiAfAFpWprq7ovP4lhWuzxh6At4DYKBPq0IqGZ9qVfM5Wq96uK6Vrltjj"
            + "QwS0nuAZC3b1MRYoumHbtRemjorLssD8Vh8TgCJd87wOXf4mSmPhdLqGbbeUksbQROHwtnbZuhL2"
            + "HGc+CqE6wBVE0oWD2JztJENj0myVQqq7fmBvs4zCb3Wh7M5AYUq8SeTmizboRML+wIF5kNUSV/wS"
            + "GG7GDx2sJDmB+AXg/jIMawL3ml7GBaeFZiB6QIDBsyxhsVx+AHl35wIDAQABo2AwXjAMBgNVHRME"
            + "BTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUXnAX3G+kB0gDN4f+PbTHINY2uNAwHwYD"
            + "VR0jBBgwFoAUKvgQWSeANR+nfLo7nyrkSqqbkuowDQYJKoZIhvcNAQEFBQADggIBAJMfxn6GXhlp"
            + "4isppcV4oOu3nloK4p7IiMrlS53363z1SQpcvCo92gzGM3qePajCTTvnRDaggOi+xcpbfJbMG62z"
            + "+e9qqKiJ53bMk+VSs3rMTRkLIhoRHmu5rIx+5r6apS4X8+g5DykaODye+sMmT0jS9OWuo8q3Ne9u"
            + "XELSwkXjcJSy3j4n+IKC+GfY8gzM130OsHcg2rzesRxNhjc2BztYdq4tge9X0Uh5dXgjTXJnu2/Q"
            + "hNvAqjJZVy7rbAHzl7DbRjQk9bFL2Snzawq/0IapfnywRD64bGoo/GRvW9Igs7eplFAhwiIRvw9u"
            + "qgEGqsk9GiduIqgTtOOT/puH/5My2DEb+faN7uEqqQT6YYH/draE5R8zYWnCHqE2yXNOyqolwP9L"
            + "OZJQunA8YBv/2rqiimvEZGR5q9F6lXpxrGAJn9tMZFNn7GmJ33Q2BrgCBkOUj+HNcXUzVzKTo/GU"
            + "O6LimPiI367viVY5IJQlQd/WHJYjK0h7OYBLCvcTXSvUt9jNoUsah9S8SqM0vyW5QvnN9KTWuUXc"
            + "XHkE3TRO0eem1viZVhcD/5V7b05Ib9vWfHONWs66JjUa83vfvajqciFdzXftDedfe0AejkKb30/J"
            + "aBKRhSo9P8l0Yiwh8t/5Wxdoar2CiEneTH7HmkbmTcTKwDqOoODA18AGnUtTmymqMIIFYzCCA0ug"
            + "AwIBAgIRAPL/UcxlhPGYCCTZhLPNvVswDQYJKoZIhvcNAQEFBQAwPDELMAkGA1UEBhMCQ04xDzAN"
            + "BgNVBAoTBkh1YXdlaTEcMBoGA1UEAxMTSHVhd2VpIEVxdWlwbWVudCBDQTAeFw0wOTEwMTkwOTAw"
            + "MjhaFw0zNDEwMTkwOTAwMDBaMDwxCzAJBgNVBAYTAkNOMQ8wDQYDVQQKEwZIdWF3ZWkxHDAaBgNV"
            + "BAMTE0h1YXdlaSBFcXVpcG1lbnQgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCi"
            + "iYQnC/Mp9obmAnXmu/Nj6rccSkEQJXlZipOv8tIjvr0B8ObpFUnU+qLojZUYlNmXH8RgRgFB1sBS"
            + "yOuGiiP0uNtJ0lPLbylsc+2fr2Rlt/qbYs1oQGz+oNl+UdAOtm/lPzggUOVVst15Ovf0Yf6LQ3CQ"
            + "alN2VJWgKpFUudDKWQ2fzbFT5YSfvhFxvtvWfgdntKAJt3sFvkKr9Qw+0EYNpQiw5EALeLWCZSYU"
            + "7A939puqYR6aNA447S1K8SgWoav82P4UY/ykLXjcgTeCnvRRtUga1gdIwm5d/vRlB5il5wspGLLe"
            + "s4SomzUYrvnvHio555NZPpvmpIXNolwvYW5opAyYzE05pVSOmHf/RY/dHto8XWexOJq/UAFBMyiH"
            + "4NT4cZpWjYWR7W9GxRXApmQrrLXte1CF/IzXWBMA2tSL0WnRJz5HRcKzsOC6FksiqsYstFjcCE7J"
            + "7Nicr3Bwq5FrZiqGSdLmLRn97XqVlWdN31HX16fzRhZMiOkvQe+uYT+BXbhU1fZIh6RRAH3V1APo"
            + "bVlCXh5PDq8Ca4dClHNHYp5RP0Pb5zBowTqBzSv7ssHrNceQsWDeNjX9t59NwviaIlXIlPiWEEJc"
            + "22XtMm4sc/+8mgOFMNXr4FWu8vdG2fgRpeWJO0E035D6TClu4So2GlN/fIccp5wVYAWF1WhxSQID"
            + "AQABo2AwXjAOBgNVHQ8BAf8EBAMCAYYwDAYDVR0TBAUwAwEB/zAdBgNVHQ4EFgQUKvgQWSeANR+n"
            + "fLo7nyrkSqqbkuowHwYDVR0jBBgwFoAUKvgQWSeANR+nfLo7nyrkSqqbkuowDQYJKoZIhvcNAQEF"
            + "BQADggIBAAALYkaoI50h81eGu+bm6W6OfXwXx2ech9r/JkYiv8NDE1gXFaqbqVTgmTMVAWIIyiYF"
            + "zFedILyhnva4zIqtBUKVTM1WU8Bx0TqLRp2/KRSX9q2AIHA7cKTYUn6XGzV4amqa3nXJ/v0q9Sty"
            + "rYqY9piARqoOTseAu4WhMQvyPgTkQ7lFJ97HOvDBM/BNFoPo9DrdLJlBaNIUngjB1c/ZkvXfDUhP"
            + "B7fegH8dY2hkGD/We0jnkEQA6ch6h/c24wJzVA9VZK6UX2KikYvFS9yipdS5ry6chRSt29UtbTEO"
            + "q4airI3U/IuxkSAEiVuasLLkGTQTJgTfroFIE0/MiTsyfmxHiMZM0vN2gaPjW+zfkxpqcQcGeNRR"
            + "jMC2Kh/bMN1is5rzoh3jWADG8tWBQjlSghxNFwAgPMV6ui3SIgNPd07LVwzMQIpMzSn670CtpGKu"
            + "KB3wchnW2JjEGd9Zb49aP1a+83pBvgUVHaZ5KTlV4lrSe/s8e3SFMiV/6p+KAnV5/cnSnuNJfl0u"
            + "Tjavw7DEqcXV6UN0Eg571WLRZvnsmCWAHncBMQ7prVDTdnc7OVsZw0TnTzcBYZtYl2mdxsR3tb3k"
            + "YngXwIxzWROeEFWpNvWnuSzEH+Vv939rdvgLzHrcYgZuvknyWx5Vp9c+ezA58JWYo/nNBFzb0/U1" + "OZck9LLi").getBytes());

    /*
     *header
     *	pvno: cmp2000 (2)
     *	sender: 4
     *		rdnSequence: 6 items (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *	recipient: 4
     *		rdnSequence: 4 items (id-at-commonName=21030533610000000012 eNodeB,id-at-organizationalUnitName=Wireless Network Product Line,id-at-organizationName=Huawei,id-at-countryName=CN)
     *	messageTime: 2011-02-22 17:56:01 (UTC)
     *	protectionAlg (shaWithRSAEncryption)
     *	transactionID: 46E72888
     *	senderNonce: 13AC3DBA7D81873B06218096A2AAE044
     *	recipNonce: 219F0452
     *body: ip (1)
     *	ip
     *		caPubs: 1 item
     *			CMPCertificate: x509v3PKCert (0)
     *				x509v3PKCert (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					signedCertificate
     *						version: v3 (2)
     *						serialNumber : 0x00b252ce935b1feb3a
     *						signature (shaWithRSAEncryption)
     *						issuer: rdnSequence: 6 items (id-at-commonName=enbroot,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *						validity
     *							notBefore: utcTime (0)	utcTime: 10-06-03 08:33:28 (UTC)
     *							notAfter: utcTime (0)	utcTime: 11-06-03 08:33:28 (UTC)
     *						subject: rdnSequence: 6 items (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *						subjectPublicKeyInfo
     *							algorithm (rsaEncryption)
     *							Padding: 0
     *							subjectPublicKey: 30818902818100CC8C0DF283FBFD3717785A4399765994A9...
     *						extensions: 3 items
     *							Extension (id-ce-subjectKeyIdentifier)
     *								Extension Id: 2.5.29.14 (id-ce-subjectKeyIdentifier)
     *								SubjectKeyIdentifier: 4C60DB752400513F2C5F659498FB55155E230045
     *							Extension (id-ce-basicConstraints)
     *								Extension Id: 2.5.29.19 (id-ce-basicConstraints)
     *								BasicConstraintsSyntax
     *									cA: True
     *							Extension (id-ce-keyUsage)
     *								Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *								Padding: 1
     *								KeyUsage: F6 (digitalSignature, contentCommitment, keyEncipherment, dataEncipherment, keyCertSign, cRLSign)
     *					algorithmIdentifier (shaWithRSAEncryption)
     *					Padding: 0
     *					encrypted: 2A69C2FD0A809383EACB7CA16E48C8ABB3E4038A4FA288B9...
     *		response: 1 item
     *			CertResponse
     *				certReqId: 355
     *				status
     *					status: accepted (0)
     *				certifiedKeyPair
     *					certOrEncCert: certificate (0)
     *						certificate: x509v3PKCert (0)
     *							x509v3PKCert (id-at-commonName=21030533610000000012 eNodeB)
     *								signedCertificate
     *									version: v3 (2)
     *									serialNumber: -141639098
     *									signature (shaWithRSAEncryption)
     *									issuer: rdnSequence: 6 items (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *									validity
     *										notBefore: utcTime (0)	utcTime: 11-02-22 17:56:01 (UTC)
     *										notAfter: utcTime (0)	utcTime: 11-06-03 08:33:28 (UTC)
     *									subject: rdnSequence (0)	rdnSequence: 1 item (id-at-commonName=21030533610000000012 eNodeB)
     *									subjectPublicKeyInfo
     *										algorithm (rsaEncryption)
     *										Padding: 0
     *										subjectPublicKey: 3082010A02820101009C2BCD07CBB0CF2B8B75062668D64F...
     *									extensions: 2 items
     *										Extension (id-ce-keyUsage)
     *											Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *											critical: True
     *											Padding: 3
     *											KeyUsage: B8 (digitalSignature, keyEncipherment, dataEncipherment, keyAgreement)
     *										Extension (id-ce-subjectAltName)
     *											Extension Id: 2.5.29.17 (id-ce-subjectAltName)
     *											critical: True
     *											GeneralNames: 1 item		dNSName: 21030533610000000012.huawei.com
     *								algorithmIdentifier (shaWithRSAEncryption)
     *								Padding: 0
     *								encrypted: 64B737A8AF0A27CB19D66D3357D35B62ECFEA26C4A589CB7...
     *	Padding: 0
     *	protection: 7C95130034E67A9E87B05B2469B4FE5523C0213A73A32C1B...
     *	extraCerts: 2 items
     *		CMPCertificate: x509v3PKCert (0)
     *			x509v3PKCert (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *				signedCertificate
     *					version: v3 (2)
     *					serialNumber : 0x00b252ce935b1feb3a
     *					signature (shaWithRSAEncryption)
     *					issuer: rdnSequence: 6 items (id-at-commonName=enbroot,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					validity
     *						notBefore: utcTime (0)		utcTime: 10-06-03 08:33:28 (UTC)
     *						notAfter: utcTime (0)		utcTime: 11-06-03 08:33:28 (UTC)
     *					subject: rdnSequence: 6 items (id-at-commonName=enbca,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					subjectPublicKeyInfo
     *						algorithm (rsaEncryption)
     *						Padding: 0
     *						subjectPublicKey: 30818902818100CC8C0DF283FBFD3717785A4399765994A9...
     *						extensions: 3 items
     *							Extension (id-ce-subjectKeyIdentifier)
     *								Extension Id: 2.5.29.14 (id-ce-subjectKeyIdentifier)
     *								SubjectKeyIdentifier: 4C60DB752400513F2C5F659498FB55155E230045
     *							Extension (id-ce-basicConstraints)
     *								Extension Id: 2.5.29.19 (id-ce-basicConstraints)	
     *								BasicConstraintsSyntax
     *									cA: True
     *							Extension (id-ce-keyUsage)
     *								Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *								Padding: 1
     *								KeyUsage: F6 (digitalSignature, contentCommitment, keyEncipherment, dataEncipherment, keyCertSign, cRLSign)
     *				algorithmIdentifier (shaWithRSAEncryption)
     *				Padding: 0
     *				encrypted: 2A69C2FD0A809383EACB7CA16E48C8ABB3E4038A4FA288B9...
     *		CMPCertificate: x509v3PKCert (0)
     *			x509v3PKCert (id-at-commonName=enbroot,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *				signedCertificate
     *					version: v3 (2)
     *					serialNumber : 0x00a1ae2a3b2800db0e
     *					signature (shaWithRSAEncryption)
     *					issuer: rdnSequence: 6 items (id-at-commonName=enbroot,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					validity
     *						notBefore: utcTime (0)		utcTime: 10-06-03 08:32:55 (UTC)
     *						notAfter: utcTime (0)		utcTime: 11-06-03 08:32:55 (UTC)
     *					subject: rdnSequence: 6 items (id-at-commonName=enbroot,id-at-organizationalUnitName=lte,id-at-organizationName=wl,id-at-localityName=qc,id-at-stateOrProvinceName=sh,id-at-countryName=cn)
     *					subjectPublicKeyInfo
     *						algorithm (rsaEncryption)
     *						Padding: 0
     *						subjectPublicKey: 30818902818100B52E31F83920EAC770A9E516A953E5F162...
     *					extensions: 3 items
     *						Extension (id-ce-subjectKeyIdentifier)
     *							Extension Id: 2.5.29.14 (id-ce-subjectKeyIdentifier)
     *							SubjectKeyIdentifier: 33C563BBADA99901734613B70E24014F5145E3C7
     *						Extension (id-ce-basicConstraints)
     *							Extension Id: 2.5.29.19 (id-ce-basicConstraints)
     *							BasicConstraintsSyntax
     *								cA: True
     *						Extension (id-ce-keyUsage)
     *							Extension Id: 2.5.29.15 (id-ce-keyUsage)
     *							Padding: 1
     *							KeyUsage: F6 (digitalSignature, contentCommitment, keyEncipherment, dataEncipherment, keyCertSign, cRLSign)
     *				algorithmIdentifier (shaWithRSAEncryption)
     *				Padding: 0
     *				encrypted: 7BD35EC086CBC4C2BF3DC891FD60341D6E3938B8ED26C4AD...
     */
    static byte[] telefonica2 = Base64
            .decode(("MIILtTCCARECAQKkVDBSMQswCQYDVQQGEwJjbjELMAkGA1UECBMCc2gxCzAJBgNVBAcTAnFjMQsw"
                    + "CQYDVQQKEwJ3bDEMMAoGA1UECxMDbHRlMQ4wDAYDVQQDEwVlbmJjYaRuMGwxCzAJBgNVBAYTAkNO"
                    + "MQ8wDQYDVQQKEwZIdWF3ZWkxJjAkBgNVBAsTHVdpcmVsZXNzIE5ldHdvcmsgUHJvZHVjdCBMaW5l"
                    + "MSQwIgYDVQQDExsyMTAzMDUzMzYxMDAwMDAwMDAxMiBlTm9kZUKgERgPMjAxMTAyMjIxNzU2MDFa"
                    + "oQ8wDQYJKoZIhvcNAQEFBQCkBgQERucoiKUSBBATrD26fYGHOwYhgJaiquBEpgYEBCGfBFKhggVD"
                    + "MIIFP6GCAmgwggJkMIICYDCCAcmgAwIBAgIJALJSzpNbH+s6MA0GCSqGSIb3DQEBBQUAMFQxCzAJ"
                    + "BgNVBAYTAmNuMQswCQYDVQQIEwJzaDELMAkGA1UEBxMCcWMxCzAJBgNVBAoTAndsMQwwCgYDVQQL"
                    + "EwNsdGUxEDAOBgNVBAMTB2VuYnJvb3QwHhcNMTAwNjAzMDgzMzI4WhcNMTEwNjAzMDgzMzI4WjBS"
                    + "MQswCQYDVQQGEwJjbjELMAkGA1UECBMCc2gxCzAJBgNVBAcTAnFjMQswCQYDVQQKEwJ3bDEMMAoG"
                    + "A1UECxMDbHRlMQ4wDAYDVQQDEwVlbmJjYTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAzIwN"
                    + "8oP7/TcXeFpDmXZZlKkeZ4/PAzRancAj6mmdhbeZY+lvgOt/KmQyolu1jPkUUDDy2nxzyuuADAQe"
                    + "C9o6VHgteppQzT2XC75ol5YUc1BtCaU2CD7MmpqFC9NB/UWCP++r1mRPXWzdI/rkhAqudfberNRX"
                    + "ouSmmHXqF0KQY+UCAwEAAaM8MDowHQYDVR0OBBYEFExg23UkAFE/LF9llJj7VRVeIwBFMAwGA1Ud"
                    + "EwQFMAMBAf8wCwYDVR0PBAQDAgH2MA0GCSqGSIb3DQEBBQUAA4GBACppwv0KgJOD6st8oW5IyKuz"
                    + "5AOKT6KIubIDsv8tRUHsodUku1ujedyMY6dzPytNHea87P3nz5Bx4gEUS7ItVmAPS1oCVrzOlrw8"
                    + "Mfd22n7w+OqL4R+9Tf3vyxIzYHCa3cR5ACgLn2p8/iRx7D+IePYz0wnrRjV3RU/JzjGY2pJQMIIC"
                    + "zzCCAssCAgFjMAMCAQAwggK+oIICujCCArYwggIfoAMCAQICBPeOwkYwDQYJKoZIhvcNAQEFBQAw"
                    + "UjELMAkGA1UEBhMCY24xCzAJBgNVBAgTAnNoMQswCQYDVQQHEwJxYzELMAkGA1UEChMCd2wxDDAK"
                    + "BgNVBAsTA2x0ZTEOMAwGA1UEAxMFZW5iY2EwHhcNMTEwMjIyMTc1NjAxWhcNMTEwNjAzMDgzMzI4"
                    + "WjAmMSQwIgYDVQQDDBsyMTAzMDUzMzYxMDAwMDAwMDAxMiBlTm9kZUIwggEiMA0GCSqGSIb3DQEB"
                    + "AQUAA4IBDwAwggEKAoIBAQCcK80Hy7DPK4t1BiZo1k8/eMBnQMv72I/Mk9odR1CLaobm2ShjLpRw"
                    + "f9aUkz3RAiCv5ccrqssItaeTTsA9/8X1QPGEKmf0FYznVzgzBIOehK8Uh2o7CxNICKu+RQCxwXc5"
                    + "5bfhGtdlEKayXmNI0Pgy5HJSwNBy3LVT9uPVTiCVB0L5HCPAFgUrYrnzS6AS7P/HidS1+rKO/sUU"
                    + "9jVy3Z7l1D/09uD+2rTqjEwu5Y7nchVFnRbAAJBtQhyvRt2V/CbtaqQckRTnVJ0QOKx9L9LKIrk1"
                    + "h+MRrYP9mIYtERSBTHm4Nzg02N0c7JxD/QvWL3No6jIptp1RkUhN1yCUdpLjAgMBAAGjQTA/MA4G"
                    + "A1UdDwEB/wQEAwIDuDAtBgNVHREBAf8EIzAhgh8yMTAzMDUzMzYxMDAwMDAwMDAxMi5odWF3ZWku"
                    + "Y29tMA0GCSqGSIb3DQEBBQUAA4GBAGS3N6ivCifLGdZtM1fTW2Ls/qJsSlict/WtdEVtThyZ51yX"
                    + "50AJsvjmQtduU4Qbj0vOPETlP9+L35j3j5Lo+RRkLFTJ4FSWZzJ6ZZSF5u3eWnMZRF74wrBg32Ip"
                    + "I9g5MA5IvyYdJb45Zcjs07QVZNQXzjBjcESwglCHC3vu4vyooIGEA4GBAHyVEwA05nqeh7BbJGm0"
                    + "/lUjwCE6c6MsGyAV6ticmTbp+BFx6fHGk1tHNNhCcJxQxSdAv9nEsClExrhuXiBSG/SdBmrAs6lh"
                    + "odMrRkMTQO/FooMiwDjRX7zNBGnVHBQYnXY/cGtTIAQWhwhFgBrq3HX31ogkEPOmBsTFeoxzYvxn"
                    + "oYIEzjCCBMowggJgMIIByaADAgECAgkAslLOk1sf6zowDQYJKoZIhvcNAQEFBQAwVDELMAkGA1UE"
                    + "BhMCY24xCzAJBgNVBAgTAnNoMQswCQYDVQQHEwJxYzELMAkGA1UEChMCd2wxDDAKBgNVBAsTA2x0"
                    + "ZTEQMA4GA1UEAxMHZW5icm9vdDAeFw0xMDA2MDMwODMzMjhaFw0xMTA2MDMwODMzMjhaMFIxCzAJ"
                    + "BgNVBAYTAmNuMQswCQYDVQQIEwJzaDELMAkGA1UEBxMCcWMxCzAJBgNVBAoTAndsMQwwCgYDVQQL"
                    + "EwNsdGUxDjAMBgNVBAMTBWVuYmNhMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDMjA3yg/v9"
                    + "Nxd4WkOZdlmUqR5nj88DNFqdwCPqaZ2Ft5lj6W+A638qZDKiW7WM+RRQMPLafHPK64AMBB4L2jpU"
                    + "eC16mlDNPZcLvmiXlhRzUG0JpTYIPsyamoUL00H9RYI/76vWZE9dbN0j+uSECq519t6s1Fei5KaY"
                    + "deoXQpBj5QIDAQABozwwOjAdBgNVHQ4EFgQUTGDbdSQAUT8sX2WUmPtVFV4jAEUwDAYDVR0TBAUw"
                    + "AwEB/zALBgNVHQ8EBAMCAfYwDQYJKoZIhvcNAQEFBQADgYEAKmnC/QqAk4Pqy3yhbkjIq7PkA4pP"
                    + "ooi5sgOy/y1FQeyh1SS7W6N53Ixjp3M/K00d5rzs/efPkHHiARRLsi1WYA9LWgJWvM6WvDwx93ba"
                    + "fvD46ovhH71N/e/LEjNgcJrdxHkAKAufanz+JHHsP4h49jPTCetGNXdFT8nOMZjaklAwggJiMIIB"
                    + "y6ADAgECAgkAoa4qOygA2w4wDQYJKoZIhvcNAQEFBQAwVDELMAkGA1UEBhMCY24xCzAJBgNVBAgT"
                    + "AnNoMQswCQYDVQQHEwJxYzELMAkGA1UEChMCd2wxDDAKBgNVBAsTA2x0ZTEQMA4GA1UEAxMHZW5i"
                    + "cm9vdDAeFw0xMDA2MDMwODMyNTVaFw0xMTA2MDMwODMyNTVaMFQxCzAJBgNVBAYTAmNuMQswCQYD"
                    + "VQQIEwJzaDELMAkGA1UEBxMCcWMxCzAJBgNVBAoTAndsMQwwCgYDVQQLEwNsdGUxEDAOBgNVBAMT"
                    + "B2VuYnJvb3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALUuMfg5IOrHcKnlFqlT5fFiKM4D"
                    + "RfpVznugWDrJtKrgr8rf9SoybAPi4JiwYHfWRAjNkutR9/h4KWbcrz1vBpooklEixtPzSUHJ4xfc"
                    + "Rz39AI0bC/qzm2ru9l1qTXMfRA2qydb0Y/Q8m2S+DyJCaiP1eNinny6u4oWxx8A6Y8mLAgMBAAGj"
                    + "PDA6MB0GA1UdDgQWBBQzxWO7ramZAXNGE7cOJAFPUUXjxzAMBgNVHRMEBTADAQH/MAsGA1UdDwQE"
                    + "AwIB9jANBgkqhkiG9w0BAQUFAAOBgQB7017AhsvEwr89yJH9YDQdbjk4uO0mxK2SKowiYNj5BoMk"
                    + "tAyjcA7hgNX00Wg7qLQe9IuoOCy2fdldmP+s7sLouXi1oh7OjOxk50TANQg4V28vPhfdgxAgGowi"
                    + "GCsbCtLscLeYallqTuvg/0O2zZITN5wcoQOjackHjIJg3eAz8A==").getBytes());

}
