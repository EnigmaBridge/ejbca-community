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

package org.ejbca.core.model.ca.publisher;

import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationSubject;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.rules.AccessRuleState;
import org.cesecore.authorization.user.AccessMatchType;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.authorization.user.matchvalues.X500PrincipalAccessMatchValue;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.mock.authentication.SimpleAuthenticationProviderSessionRemote;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.roles.RoleData;
import org.cesecore.roles.management.RoleManagementSessionRemote;
import org.cesecore.util.Base64;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ca.publisher.PublisherProxySessionRemote;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionRemote;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;



/**
 * Tests Publishers.
 *
 * @version $Id: PublisherTest.java 21141 2015-04-27 11:27:26Z mikekushner $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PublisherTest {

	private static final byte[] testcert = Base64.decode(("MIICWzCCAcSgAwIBAgIIJND6Haa3NoAwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
			+ "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAyMDEw"
			+ "ODA5MTE1MloXDTA0MDEwODA5MjE1MlowLzEPMA0GA1UEAxMGMjUxMzQ3MQ8wDQYD"
			+ "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB"
			+ "hwKBgQCQ3UA+nIHECJ79S5VwI8WFLJbAByAnn1k/JEX2/a0nsc2/K3GYzHFItPjy"
			+ "Bv5zUccPLbRmkdMlCD1rOcgcR9mmmjMQrbWbWp+iRg0WyCktWb/wUS8uNNuGQYQe"
			+ "ACl11SAHFX+u9JUUfSppg7SpqFhSgMlvyU/FiGLVEHDchJEdGQIBEaOBgTB/MA8G"
			+ "A1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUyxKILxFM"
			+ "MNujjNnbeFpnPgB76UYwHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmsw"
			+ "GwYDVR0RBBQwEoEQMjUxMzQ3QGFuYXRvbS5zZTANBgkqhkiG9w0BAQUFAAOBgQAS"
			+ "5wSOJhoVJSaEGHMPw6t3e+CbnEL9Yh5GlgxVAJCmIqhoScTMiov3QpDRHOZlZ15c"
			+ "UlqugRBtORuA9xnLkrdxYNCHmX6aJTfjdIW61+o/ovP0yz6ulBkqcKzopAZLirX+"
			+ "XSWf2uI9miNtxYMVnbQ1KPdEAt7Za3OQR6zcS0lGKg==").getBytes());

	private static final byte[] testcrl = Base64.decode(("MIIDEzCCAnwCAQEwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UEAxMGVGVzdENBMQ8w"
			+ "DQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFFw0wMjAxMDMxMjExMTFaFw0wMjAx"
			+ "MDIxMjExMTFaMIIB5jAZAggfi2rKt4IrZhcNMDIwMTAzMTIxMDUxWjAZAghAxdYk"
			+ "7mJxkxcNMDIwMTAzMTIxMDUxWjAZAgg+lCCL+jumXxcNMDIwMTAzMTIxMDUyWjAZ"
			+ "Agh4AAPpzSk/+hcNMDIwMTAzMTIxMDUyWjAZAghkhx9SFvxAgxcNMDIwMTAzMTIx"
			+ "MDUyWjAZAggj4g5SUqaGvBcNMDIwMTAzMTIxMDUyWjAZAghT+nqB0c6vghcNMDIw"
			+ "MTAzMTE1MzMzWjAZAghsBWMAA55+7BcNMDIwMTAzMTE1MzMzWjAZAgg8h0t6rKQY"
			+ "ZhcNMDIwMTAzMTE1MzMzWjAZAgh7KFsd40ICwhcNMDIwMTAzMTE1MzM0WjAZAggA"
			+ "kFlDNU8ubxcNMDIwMTAzMTE1MzM0WjAZAghyQfo1XNl0EBcNMDIwMTAzMTE1MzM0"
			+ "WjAZAggC5Pz7wI/29hcNMDIwMTAyMTY1NDMzWjAZAggEWvzRRpFGoRcNMDIwMTAy"
			+ "MTY1NDMzWjAZAggC7Q2W0iXswRcNMDIwMTAyMTY1NDMzWjAZAghrfwG3t6vCiBcN"
			+ "MDIwMTAyMTY1NDMzWjAZAgg5C+4zxDGEjhcNMDIwMTAyMTY1NDMzWjAZAggX/olM"
			+ "45KxnxcNMDIwMTAyMTY1NDMzWqAvMC0wHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsP"
			+ "WFzafOFgLmswCgYDVR0UBAMCAQQwDQYJKoZIhvcNAQEFBQADgYEAPvYDZofCOopw"
			+ "OCKVGaK1aPpHkJmu5Xi1XtRGO9DhmnSZ28hrNu1A5R8OQI43Z7xFx8YK3S56GRuY"
			+ "0EGU/RgM3AWhyTAps66tdyipRavKmH6MMrN4ypW/qbhsd4o8JE9pxxn9zsQaNxYZ"
			+ "SNbXM2/YxkdoRSjkrbb9DUdCmCR/kEA=").getBytes());

	private final static String cloneName = "TESTCLONEDUMMYCUSTOM";
	private final static String orgName = "TESTDUMMYCUSTOM";
	private final static String newName = "TESTNEWDUMMYCUSTOM";

	private static final Logger log = Logger.getLogger(PublisherTest.class);

	private static final AuthenticationToken internalAdmin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("PublisherTest"));

	private final static String commonname = PublisherTest.class.getCanonicalName();

	private final static Set<String> publisherNames = new HashSet<String>();

	private final PublisherSessionRemote publisherSession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublisherSessionRemote.class);
	private final PublisherProxySessionRemote publisherProxySession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublisherProxySessionRemote.class, EjbRemoteHelper.MODULE_TEST);
	private final RoleManagementSessionRemote roleManagementSession = EjbRemoteHelper.INSTANCE.getRemoteSession(RoleManagementSessionRemote.class);
	private final SimpleAuthenticationProviderSessionRemote simpleAuthenticationProvider = EjbRemoteHelper.INSTANCE.getRemoteSession(SimpleAuthenticationProviderSessionRemote.class, EjbRemoteHelper.MODULE_TEST);

	private AuthenticationToken admin;

	@BeforeClass
	public static void beforeClass() throws Exception {
		CryptoProviderTools.installBCProvider();
		publisherNames.clear();
	}

	@Before
	public void setUp() throws Exception {
		Certificate cert = CertTools.getCertfromByteArray(testcert);
		int caid = CertTools.getIssuerDN(cert).hashCode();
		String subjectDn = CertTools.getSubjectDN(cert);
		String cN = CertTools.getPartsFromDN(subjectDn, "CN").get(0);

		Set<Principal> principals = new HashSet<Principal>();
		Set<Certificate> credentials = new HashSet<Certificate>();
		credentials.add(cert);
		X500Principal p = new X500Principal(subjectDn);
		principals.add(p);
		AuthenticationSubject subject = new AuthenticationSubject(principals, credentials);
		this.admin = this.simpleAuthenticationProvider.authenticate(subject);

		RoleData role = this.roleManagementSession.create(internalAdmin, commonname);
		Collection<AccessRuleData> rules = new ArrayList<AccessRuleData>();
		rules.add(new AccessRuleData(commonname, StandardRules.CAACCESS.resource() + caid, AccessRuleState.RULE_ACCEPT, false));
		role = this.roleManagementSession.addAccessRulesToRole(internalAdmin, role, rules);
		Collection<AccessUserAspectData> users = new ArrayList<AccessUserAspectData>();
		users.add(new AccessUserAspectData(commonname, caid, X500PrincipalAccessMatchValue.WITH_COMMONNAME, AccessMatchType.TYPE_EQUALCASE, cN));
		role = this.roleManagementSession.addSubjectsToRole(internalAdmin, role, users);
	}

	@After
	public void tearDown() throws Exception {
		this.roleManagementSession.remove(internalAdmin, commonname);
	}


	/**
	 * adds ldap publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test01AddLDAPPublisher() throws AuthorizationDeniedException {
		log.trace(">test01AddLDAPPublisher()");
		try {
			LdapPublisher publisher = new LdapPublisher();
			publisher.setHostnames("localhost");
			publisher.setDescription("Used in Junit Test, Remove this one");
			final String publisherName = "TESTLDAP";
			publisherNames.add(publisherName);
			this.publisherProxySession.addPublisher(internalAdmin, publisherName, publisher);
		} catch (PublisherExistsException pee) {
			final String m = "The name of the publisher does already exist for another publisher.";
			log.error(m, pee);
			assertTrue(m, false);
		}
		log.trace("<test01AddLDAPPublisher()");
	}

	/**
	 * adds ad publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test02AddADPublisher() throws AuthorizationDeniedException {
		log.trace(">test02AddADPublisher() ");
		try {
			ActiveDirectoryPublisher publisher = new ActiveDirectoryPublisher();
			publisher.setHostnames("localhost");
			publisher.setDescription("Used in Junit Test, Remove this one");
			final String publisherName = "TESTAD";
			publisherNames.add(publisherName);
			this.publisherProxySession.addPublisher(internalAdmin, publisherName, publisher);
		} catch (PublisherExistsException pee) {
			final String m = "The name of the publisher does already exist for another publisher.";
			log.error(m, pee);
			assertTrue(m, false);
		}
		log.trace("<test02AddADPublisher() ");
	}

	/**
	 * adds custom publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test03AddCustomPublisher() throws AuthorizationDeniedException {
		log.trace(">test03AddCustomPublisher()");
		try {
		    CustomPublisherContainer publisher = new CustomPublisherContainer();
			publisher.setClassPath("org.ejbca.core.model.ca.publisher.DummyCustomPublisher");
			publisher.setDescription("Used in Junit Test, Remove this one");
			this.publisherProxySession.addPublisher(internalAdmin, orgName, publisher);
		} catch (PublisherExistsException pee) {
			final String m = "The name of the publisher does already exist for another publisher.";
			log.error(m, pee);
			assertTrue(m, false);
		}
		log.trace("<test03AddCustomPublisher()");
	}

	/**
	 * renames publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test04RenamePublisher() throws AuthorizationDeniedException {
		log.trace(">test04RenamePublisher()");
		try {
			publisherNames.add(newName);
			this.publisherProxySession.renamePublisher(internalAdmin, orgName, newName);
		} catch (PublisherExistsException pee) {
			final String m = "The new name of the publisher does already exist for another publisher.";
			log.error(m, pee);
			assertTrue(m, false);
		}
		log.trace("<test04RenamePublisher()");
	}

	/**
	 * clones publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test05ClonePublisher() throws AuthorizationDeniedException {
		log.trace(">test05ClonePublisher()");

		publisherNames.add(cloneName);
		try {
			this.publisherProxySession.clonePublisher(internalAdmin, newName, cloneName);
		} catch (PublisherDoesntExistsException e) {
			final String m = "Publisher to be cloned does not exist.";
			log.error(m, e);
			assertTrue(m, false);
		} catch (PublisherExistsException e) {
			final String m = "Publisher clone target does already exists..";
			log.error(m, e);
			assertTrue(m, false);
		}
		log.trace("<test05ClonePublisher()");
	}

	/**
	 * edits publisher
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test06EditPublisher() throws AuthorizationDeniedException {
		log.trace(">test06EditPublisher()");

		final BasePublisher publisher = this.publisherSession.getPublisher(internalAdmin, cloneName);
		publisher.setDescription(publisher.getDescription().toUpperCase());
		this.publisherSession.changePublisher(internalAdmin, cloneName, publisher);

		log.trace("<test06EditPublisher()");
	}

	/**
	 * stores a cert to the dummy publisher
	 * @throws CertificateException 
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test07StoreCertToDummy() throws CertificateException, AuthorizationDeniedException {
		log.trace(">test07StoreCertToDummy()");
		final Certificate cert = CertTools.getCertfromByteArray(testcert);
		final ArrayList<Integer> publishers = new ArrayList<Integer>();
		publishers.add(Integer.valueOf(this.publisherProxySession.getPublisherId(newName)));

		final boolean ret = this.publisherSession.storeCertificate(
				this.admin, publishers, cert, "test05", "foo123", null, null,
				CertificateConstants.CERT_ACTIVE,
				CertificateConstants.CERTTYPE_ENDENTITY,
				-1, RevokedCertInfo.NOT_REVOKED, "foo",
				CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, new Date().getTime(), null);
		assertTrue("Storing certificate to dummy publisher failed", ret);
		log.trace("<test07StoreCertToDummyr()");
	}

	/**
	 * stores a cert to the dummy publisher
	 * @throws CRLException 
	 * @throws AuthorizationDeniedException 
	 */
	@Test
	public void test08storeCRLToDummy() throws CRLException, AuthorizationDeniedException {
		log.trace(">test08storeCRLToDummy()");
		final String issuerDn = CertTools.getIssuerDN(CertTools.getCRLfromByteArray(testcrl));
		final ArrayList<Integer> publishers = new ArrayList<Integer>();
		publishers.add(Integer.valueOf(this.publisherProxySession.getPublisherId(newName)));
		final boolean ret = this.publisherSession.storeCRL(this.admin, publishers, testcrl, null, 1, issuerDn);
		assertTrue("Storing CRL to dummy publisher failed", ret);

		log.trace("<test08storeCRLToDummy()");
	}

    @Test
    public void testParallelPublishing() throws Exception {
        final String TESTNAME = PublisherTest.class.getSimpleName() + "_testParallelPublishing";
        final CustomPublisherContainer publisher = new CustomPublisherContainer();
        publisher.setClassPath(DummyCustomPublisher.class.getName());
        publisher.setDescription("Used in Junit Test '"+ TESTNAME + "'. Remove this one.");
        final List<Integer> publishers = new ArrayList<Integer>();
        for (int i=0; i<30; i++) {
            final String PUBLISHER_NAME = TESTNAME + i;
            publisherNames.add(PUBLISHER_NAME); // For cleanup in @AfterClass
            publisherProxySession.addPublisher(internalAdmin, PUBLISHER_NAME, publisher);
            final int publisherId = publisherProxySession.getPublisherId(PUBLISHER_NAME);
            publisherProxySession.testConnection(publisherId);
            publishers.add(publisherId);
        }
        final Certificate testCertificate = CertTools.getCertfromByteArray(testcert);
        final String cafp = "CA fingerprint could be anything in this test.";
        final boolean ret = publisherSession.storeCertificate(this.admin, publishers, testCertificate, "username", "foo123", null, cafp,
                CertificateConstants.CERT_ACTIVE, CertificateConstants.CERTTYPE_ENDENTITY, -1,  RevokedCertInfo.NOT_REVOKED, "tag",
                CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, System.currentTimeMillis(), null);
        assertTrue("Unable to store certificate for " + publishers.size() + " publishers in one call.", ret);
    }

	/**
	 * removes all publishers
	 */
	@AfterClass
	public static void removePublishers() {
		final PublisherProxySessionRemote publisherProxySession = EjbRemoteHelper.INSTANCE.getRemoteSession(PublisherProxySessionRemote.class, EjbRemoteHelper.MODULE_TEST);
		boolean ret = true;
		for( final String publisherName : publisherNames ) {
			try {
				publisherProxySession.removePublisher(internalAdmin, publisherName);
				log.debug("Publisher named '"+publisherName+"' removed.");
			} catch (Exception pee) {ret = false;}
		}
		assertTrue("Removing Publisher failed", ret);
	}

} // TestPublisher
