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
package org.ejbca.core.protocol.ws;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.CADoesntExistsException_Exception;
import org.ejbca.core.protocol.ws.client.gen.CertificateResponse;
import org.ejbca.core.protocol.ws.client.gen.CesecoreException_Exception;
import org.ejbca.core.protocol.ws.client.gen.EjbcaException_Exception;
import org.ejbca.core.protocol.ws.client.gen.ExtendedInformationWS;
import org.ejbca.core.protocol.ws.client.gen.NotFoundException_Exception;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test of certificate extensions with values from WS.
 * 
 * @version $Id: CertificateExtensionTest.java 20992 2015-03-25 10:05:53Z mikekushner $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CertificateExtensionTest extends CommonEjbcaWS {

	private static final Logger log = Logger.getLogger(CertificateExtensionTest.class);
	private final String wsadminRoleName = "CertificateExtensionTest";	
	private static final String CERTIFICATE_PROFILE = "certExtension";
	private static final String TEST_USER = "certExtension";
	private static final String END_ENTITY_PROFILE = "endEntityProfile";
	private static final String sOID_one = "1.2.3.4";
	private static final String sOID_several = "1.2.3.5";
	private static final int nrOfValues = 3;
	private static final Random random = new Random();

    private final CertificateProfileSessionRemote certificateProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class);
    private final EndEntityManagementSessionRemote endEntityManagementSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class);
    private final EndEntityProfileSessionRemote endEntityProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);
    
	@BeforeClass
	public static void setupAccessRights() {
		adminBeforeClass();
	}

	@Before
	public void setUpAdmin() throws Exception {
		adminSetUpAdmin();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

    @Test
    public void test00SetupAccessRights() throws Exception {
        super.setupAccessRights(this.wsadminRoleName);
    }

	@Test
	public void test01AddProfiles() throws Exception {
		if (this.certificateProfileSession.getCertificateProfileId(CERTIFICATE_PROFILE) != 0) {
			this.certificateProfileSession.removeCertificateProfile(intAdmin, CERTIFICATE_PROFILE);
		}
		final int certProfID; {
			final CertificateProfile profile = new CertificateProfile(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER);
			final List<Integer> usedCertificateExtensions = new LinkedList<Integer>();
			usedCertificateExtensions.add(Integer.valueOf(1));
			usedCertificateExtensions.add(Integer.valueOf(2));
			profile.setUsedCertificateExtensions(usedCertificateExtensions);
			this.certificateProfileSession.addCertificateProfile(intAdmin, CERTIFICATE_PROFILE, profile);
			certProfID = this.certificateProfileSession.getCertificateProfileId(CERTIFICATE_PROFILE);
		}
		if ( this.endEntityProfileSession.getEndEntityProfile(END_ENTITY_PROFILE)!=null ) {
			this.endEntityProfileSession.removeEndEntityProfile(intAdmin, END_ENTITY_PROFILE);
		}
		{
			final EndEntityProfile profile = new EndEntityProfile(true);
			profile.setValue(EndEntityProfile.AVAILCERTPROFILES, 0, Integer.toString(certProfID));
			this.endEntityProfileSession.addEndEntityProfile(intAdmin, END_ENTITY_PROFILE, profile);
		}
	}

	@Test
	public void test02GetCertSuccess() throws Exception {
		getCertificateWithExtension(true);
	}

	@Test
	public void test03GetCertFail() throws Exception {
		getCertificateWithExtension(false);
	}

	@Test
	public void test04SubjectAltNameExtensionTest() throws Exception {
	       if (this.certificateProfileSession.getCertificateProfileId(CERTIFICATE_PROFILE) != 0) {
	            this.certificateProfileSession.removeCertificateProfile(intAdmin, CERTIFICATE_PROFILE);
	        }
	        final int ctpid; {
	            final CertificateProfile profile = new CertificateProfile(CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER);
	            profile.setUseCertificateTransparencyInCerts(true);
	            this.certificateProfileSession.addCertificateProfile(intAdmin, CERTIFICATE_PROFILE, profile);
	            ctpid = this.certificateProfileSession.getCertificateProfileId(CERTIFICATE_PROFILE);
	        }
	        if ( this.endEntityProfileSession.getEndEntityProfile(END_ENTITY_PROFILE)!=null ) {
	            this.endEntityProfileSession.removeEndEntityProfile(intAdmin, END_ENTITY_PROFILE);
	        }
	        {
	            final EndEntityProfile profile = new EndEntityProfile(true);
	            profile.setValue(EndEntityProfile.AVAILCERTPROFILES, 0, Integer.toString(ctpid));
	            this.endEntityProfileSession.addEndEntityProfile(intAdmin, END_ENTITY_PROFILE, profile);
	        }

	       final UserDataVOWS userData = new UserDataVOWS(TEST_USER, PASSWORD, true, "C=SE, CN=cert extension test",
	                getAdminCAName(), "DNSName=(top.secret).domain.se", "foo@anatom.se", UserDataVOWS.STATUS_NEW,
	                UserDataVOWS.TOKEN_TYPE_USERGENERATED, END_ENTITY_PROFILE, CERTIFICATE_PROFILE, null);
	        this.ejbcaraws.editUser(userData);
	        
	        
	        final X509Certificate cert = getMyCertificate();
	        assertNotNull(cert);
	        assertEquals("dNSName=top.secret.domain.se", CertTools.getSubjectAlternativeName(cert));  
	        
	        // Check that the number-of-redacted-lables-extension was added
	        byte[] extValue = cert.getExtensionValue("1.3.6.1.4.1.11129.2.4.6");
	        assertNotNull("Number of redacted labels (1.3.6.1.4.1.11129.2.4.6) didn't get added", extValue);
	        
	        // Check that the number-of-redacted-lables-extension has the write value
	        DEROctetString octs = ((DEROctetString) DEROctetString.fromByteArray(extValue));
	        ASN1Sequence seq = (ASN1Sequence) DERSequence.fromByteArray(octs.getOctets());
	        assertEquals(1, seq.size());
	        assertEquals("2", seq.getObjectAt(0).toString());    
	}
	
	@Test
	public void test99cleanUpAdmins() {
	    try {
	        this.certificateProfileSession.removeCertificateProfile(intAdmin, CERTIFICATE_PROFILE);
	    } catch (Exception e) {
	        // do nothing
	    }
	    try {
	        this.endEntityProfileSession.removeEndEntityProfile(intAdmin, END_ENTITY_PROFILE);
	    } catch (Exception e) {
	        // do nothing
	    }
	    try {
	        this.endEntityManagementSession.revokeAndDeleteUser(intAdmin, TEST_USER, RevokedCertInfo.REVOCATION_REASON_UNSPECIFIED);
	    } catch (Exception e) {
	        // do nothing
	    }
	    try {
	        super.cleanUpAdmins(this.wsadminRoleName);
	    } catch (Exception e) {
	        // do nothing
	    }
	}

	private void getCertificateWithExtension(boolean isExpectedToWork) throws Exception {

		final byte[]values[] = getRandomValues(nrOfValues);
		final byte[]value[] = isExpectedToWork ? getRandomValues(1) : new byte[1][0];

		editUser(values, value[0]);
		final X509Certificate cert = getMyCertificate();
		if ( cert==null ) {
			assertFalse(isExpectedToWork);
			return;
		}
		assertTrue(isExpectedToWork);
		checkExtension( value, cert.getExtensionValue(sOID_one), sOID_one );
		checkExtension( values, cert.getExtensionValue(sOID_several), sOID_several );
	}
	private void checkExtension(byte[] values[], byte extension[], String sOID) throws IOException {
		assertNotNull(getNoCertExtensionProperties(sOID), extension);
		final byte octets[]; {
			final ASN1Primitive asn1o = ASN1Primitive.fromByteArray(extension);
			assertNotNull(asn1o);
			log.info("The extension for the OID '"+sOID+"' of class '"+asn1o.getClass().getCanonicalName()+ "' is: "+asn1o);
			assertTrue(asn1o instanceof ASN1OctetString);
			octets = ((ASN1OctetString)asn1o).getOctets();
			if ( values.length==1 ) {
				assertArrayEquals( (new DEROctetString(values[0])).getEncoded(), octets);
				return;
			}
		}
		final ASN1Sequence seq; {
			final ASN1Primitive asn1o = ASN1Primitive.fromByteArray(octets);
			log.info("The contents of the '"+sOID+"' can be decoded to a '"+asn1o.getClass().getCanonicalName()+ "' class.");
			assertTrue(asn1o instanceof ASN1Sequence);
			seq= (ASN1Sequence)asn1o;
		}
		assertEquals( values.length, seq.size() );
		for ( int i=0; i<seq.size(); i++ ) {
			final ASN1Primitive derO = seq.getObjectAt(i).toASN1Primitive();
			assertTrue(derO instanceof ASN1OctetString);
			assertArrayEquals((new DEROctetString(values[i])).getEncoded(), ((ASN1OctetString)derO).getOctets());
		}
	}
	private byte[][] getRandomValues( int nr) {
		final byte values[][] = new byte[nr][400];
		for ( int i=0; i<nr; i++ ) {
			random.nextBytes(values[i]);
		}
		return values;
	}
	private void editUser( byte[] values[], byte value[] ) throws Exception {
		final UserDataVOWS userData = new UserDataVOWS(TEST_USER, PASSWORD, true, "C=SE, CN=cert extension test",
				getAdminCAName(), null, "foo@anatom.se", UserDataVOWS.STATUS_NEW,
				UserDataVOWS.TOKEN_TYPE_USERGENERATED, END_ENTITY_PROFILE, CERTIFICATE_PROFILE, null);
		final List<ExtendedInformationWS> lei = new LinkedList<ExtendedInformationWS>();
		for( int i=0; i<values.length; i++ ){
			final ExtendedInformationWS ei = new ExtendedInformationWS();
			ei.setName( sOID_several + ".value" + Integer.toString(i+1) );
			ei.setValue(new String( Hex.encode( (new DEROctetString(values[i])).getEncoded() ) ));
			lei.add(ei);
		}
		if ( value!=null && value.length > 0){
			final ExtendedInformationWS ei = new ExtendedInformationWS();
			ei.setName( sOID_one );
			ei.setValue(new String( Hex.encode( (new DEROctetString(value)).getEncoded() ) ));
			lei.add(ei);
		}
		userData.setExtendedInformation(lei);
		this.ejbcaraws.editUser(userData);
	}
	private X509Certificate getMyCertificate() throws GeneralSecurityException, AuthorizationDeniedException_Exception, CADoesntExistsException_Exception, NotFoundException_Exception, CesecoreException_Exception, IOException, OperatorCreationException {
		final KeyPair keys = KeyTools.genKeys("1024", AlgorithmConstants.KEYALGORITHM_RSA);
		final PKCS10CertificationRequest pkcs10 = CertTools.genPKCS10CertificationRequest("SHA1WithRSA", CertTools.stringToBcX500Name("CN=NOUSED"), keys.getPublic(),
				new DERSet(), keys.getPrivate(), null);

		final CertificateResponse certenv;
		try {
			certenv = this.ejbcaraws.pkcs10Request(TEST_USER, PASSWORD, new String(Base64.encode(pkcs10.getEncoded())), null,
					CertificateHelper.RESPONSETYPE_CERTIFICATE);
		} catch (EjbcaException_Exception e) {
			return null;
		} catch (SOAPFaultException e) {
			return null;
		}
		assertNotNull(certenv);
		assertTrue(certenv.getResponseType().equals(CertificateHelper.RESPONSETYPE_CERTIFICATE));
		return (X509Certificate)CertificateHelper.getCertificate(certenv.getData());
	}
	private String getNoCertExtensionProperties(String sOID) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		pw.println("No '"+sOID+"' extension in generated certificate.");
		pw.println("The reason might be that '"+sOID+"' is not defined in the file src/java/certextensions.properties .");
		pw.println("The files should look something like this:");
		pw.println();
		pw.println("id1.oid = "+sOID_one);
		pw.println("id1.classpath=org.cesecore.certificates.certificate.certextensions.BasicCertificateExtension");
		pw.println("id1.displayname=SingleExtension");
		pw.println("id1.used=true");
		pw.println("id1.translatable=false");
		pw.println("id1.critical=false");
		pw.println("id1.property.dynamic=true");
		pw.println("id1.property.encoding=RAW");
		pw.println();
		pw.println("id2.oid = "+sOID_several);
		pw.println("id2.classpath=org.cesecore.certificates.certificate.certextensions.BasicCertificateExtension");
		pw.println("id2.displayname=MultipleExtension");
		pw.println("id2.used=true");
		pw.println("id2.translatable=false");
		pw.println("id2.critical=false");
		pw.println("id2.property.dynamic=true");
		pw.println("id2.property.nvalues="+nrOfValues);
		pw.println("id2.property.encoding=DEROCTETSTRING");
		pw.flush();
		return sw.toString();
	}
	
	@Override
	public String getRoleName() {
		return this.wsadminRoleName+"Mgmt";
	}
}
