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

package org.ejbca.core.ejb.ra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.ErrorCode;
import org.cesecore.authentication.tokens.AuthenticationSubject;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionRemote;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.rules.AccessRuleState;
import org.cesecore.authorization.user.AccessMatchType;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.authorization.user.matchvalues.X500PrincipalAccessMatchValue;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.certificate.CertificateStatus;
import org.cesecore.certificates.certificate.CertificateStoreSessionRemote;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.certificates.util.DnComponents;
import org.cesecore.configuration.GlobalConfigurationSessionRemote;
import org.cesecore.keys.util.KeyTools;
import org.cesecore.keys.util.PublicKeyWrapper;
import org.cesecore.mock.authentication.SimpleAuthenticationProviderSessionRemote;
import org.cesecore.mock.authentication.tokens.TestAlwaysAllowLocalAuthenticationToken;
import org.cesecore.mock.authentication.tokens.TestX509CertificateAuthenticationToken;
import org.cesecore.roles.RoleData;
import org.cesecore.roles.access.RoleAccessSessionRemote;
import org.cesecore.roles.management.RoleManagementSessionRemote;
import org.cesecore.util.CertTools;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ca.CaTestCase;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionRemote;
import org.ejbca.core.ejb.ca.sign.SignSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ra.AlreadyRevokedException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.util.query.BasicMatch;
import org.ejbca.util.query.Query;
import org.ejbca.util.query.UserMatch;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Tests the EndEntityInformation entity bean and some parts of EndEntityManagementSession.
 * 
 * @version $Id: EndEntityManagementSessionTest.java 20728 2015-02-20 14:55:55Z mikekushner $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EndEntityManagementSessionTest extends CaTestCase {

    private static final Logger log = Logger.getLogger(EndEntityManagementSessionTest.class);
    private static final AuthenticationToken admin = new TestAlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("EndEntityManagementSessionTest"));
    private int caid = getTestCAId();

    private static String username;
    private static String pwd;
    private static ArrayList<String> usernames = new ArrayList<String>();
    private static String serialnumber;

    private CAAdminSessionRemote caAdminSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CAAdminSessionRemote.class);
    private CaSessionRemote caSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class);
    private EndEntityAccessSessionRemote endEntityAccessSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityAccessSessionRemote.class);
    private EndEntityProfileSessionRemote endEntityProfileSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class);;
    private EndEntityManagementSessionRemote endEntityManagementSession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class);
    private CertificateStoreSessionRemote storeSession = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateStoreSessionRemote.class);
    private SignSessionRemote signSession = EjbRemoteHelper.INSTANCE.getRemoteSession(SignSessionRemote.class);
    private SimpleAuthenticationProviderSessionRemote simpleAuthenticationProvider = EjbRemoteHelper.INSTANCE.getRemoteSession(SimpleAuthenticationProviderSessionRemote.class, EjbRemoteHelper.MODULE_TEST);
    private RoleManagementSessionRemote roleManagementSession = EjbRemoteHelper.INSTANCE.getRemoteSession(RoleManagementSessionRemote.class);
    private RoleAccessSessionRemote roleAccessSession = EjbRemoteHelper.INSTANCE.getRemoteSession(RoleAccessSessionRemote.class);
    private AccessControlSessionRemote accessControlSession = EjbRemoteHelper.INSTANCE.getRemoteSession(AccessControlSessionRemote.class);
    private GlobalConfigurationSessionRemote globalConfSession = EjbRemoteHelper.INSTANCE.getRemoteSession(GlobalConfigurationSessionRemote.class);
    private EndEntityManagementProxySessionRemote endEntityManagementProxySession = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementProxySessionRemote.class, EjbRemoteHelper.MODULE_TEST); 

    @BeforeClass
    public static void beforeClass() {
        CryptoProviderTools.installBCProviderIfNotAvailable();
        
        // Make user that we know later...
        username = genRandomUserName();
        pwd = genRandomPwd();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < usernames.size(); i++) {
            try {
                endEntityManagementSession.deleteUser(admin, (String) usernames.get(i));
            } catch (Exception e) {
            } // NOPMD, ignore errors so we don't stop deleting users because
              // one of them does not exist.
        }
        try {
            endEntityProfileSession.removeEndEntityProfile(admin, "TESTMERGEWITHWS");
        } catch (Exception e) {} // NOPMD, ignore errors
    }
    
    public String getRoleName() {
        return this.getClass().getSimpleName(); 
    }

    private void genRandomSerialnumber() throws Exception {
        // Gen random number
        Random rand = new Random(new Date().getTime() + 4913);
        serialnumber = "";
        for (int i = 0; i < 8; i++) {
            int randint = rand.nextInt(9);
            serialnumber += (Integer.valueOf(randint)).toString();
        }
        log.debug("Generated random serialnumber: serialnumber =" + serialnumber);

    } // genRandomSerialnumber

    /**
     * tests creation of new user and duplicate user
     * 
     * @throws Exception error
     */
    private void addUser() throws Exception {
        log.trace(">addUser()");

        String email = username + "@anatom.se";
        endEntityManagementSession.addUser(admin, username, pwd, "C=SE, O=AnaTom, CN=" + username, "rfc822name=" + email, email, true,
                SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
        usernames.add(username);
        log.debug("created user: " + username + ", " + pwd + ", C=SE, O=AnaTom, CN=" + username);
        // Add the same user again
        boolean userexists = false;
        try {
            endEntityManagementSession.addUser(admin, username, pwd, "C=SE, O=AnaTom, CN=" + username, "rfc822name=" + email, email, true,
                    SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
        } catch (EndEntityExistsException e) {
            userexists = true; // This is what we want
        }
        assertTrue("Trying to create the same user twice didn't throw EndEntityExistsException", userexists);

        // try to add user with non-existing CA-id
        String username2 = genRandomUserName();
        int fakecaid = -1;
        boolean thrown = false;
        try {
            endEntityManagementSession.addUser(admin, username2, pwd, "C=SE, O=AnaTom, CN=" + username2, null, null, true, SecConst.EMPTY_ENDENTITYPROFILE,
                    CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, fakecaid);
            assertTrue(false);
        } catch (CADoesntExistsException e) {
            thrown = true;
        }
        assertTrue(thrown);

        log.trace("<addUser()");
    }

    /**
     * tests creation of new user testing behavior of empty passwords
     * 
     * @throws Exception error
     */
    @Test
    public void testAddUserWithEmptyPwd() throws Exception {
        // First make sure we have end entity profile limitations enabled
        final GlobalConfiguration gc = (GlobalConfiguration) globalConfSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
        final boolean eelimitation = gc.getEnableEndEntityProfileLimitations();
        gc.setEnableEndEntityProfileLimitations(true);
        globalConfSession.saveConfiguration(roleMgmgToken, gc);   
        final String eeprofileName = "TESTADDUSER";
        try {            
            // Add a new end entity profile, by default password is required and we should not be able to add a user with empty or null password.
            EndEntityProfile profile = new EndEntityProfile();
            profile.addField(DnComponents.COMMONNAME);
            profile.addField(DnComponents.COUNTRY);
            profile.setValue(EndEntityProfile.AVAILCAS, 0, Integer.toString(SecConst.ALLCAS));
            profile.setAllowMergeDnWebServices(true);
            // Profile will be removed in finally clause
            endEntityProfileSession.addEndEntityProfile(admin, eeprofileName, profile);
            int profileId = endEntityProfileSession.getEndEntityProfileId(eeprofileName);
            String thisusername = genRandomUserName();
            String email = thisusername + "@anatom.se";
            try {
                endEntityManagementSession.addUser(admin, thisusername, "", "C=SE, CN=" + thisusername, null, email, false,
                        profileId, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
                usernames.add(thisusername);
                fail("User " + thisusername + " was added to the database although it should not have been.");
            } catch (UserDoesntFullfillEndEntityProfile e) {
                assertTrue("Error message should be about password", e.getMessage().contains("Password cannot be empty or null"));
            }
            try {
                endEntityManagementSession.addUser(admin, thisusername, null, "C=SE, CN=" + thisusername, null, email, false,
                        profileId, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
                usernames.add(thisusername);
                fail("User " + thisusername + " was added to the database although it should not have been.");
            } catch (UserDoesntFullfillEndEntityProfile e) {
                assertTrue("Error message should be about password", e.getMessage().contains("Password cannot be empty or null"));
            }
            // Set required = false for password, then an empty password should be allowed
            profile.setRequired(EndEntityProfile.PASSWORD,0,false);
            endEntityProfileSession.changeEndEntityProfile(admin, eeprofileName, profile);
            try {
                endEntityManagementSession.addUser(admin, thisusername, "", "C=SE, CN=" + thisusername, null, email, false,
                        profileId, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
                usernames.add(thisusername);
            } catch (UserDoesntFullfillEndEntityProfile e) {
                fail("User " + thisusername + " was not added to the database although it should have been.");
            }
            thisusername = genRandomUserName();
            email = thisusername + "@anatom.se";
            try {
                endEntityManagementSession.addUser(admin, thisusername, null, "C=SE, CN=" + thisusername, null, email, false,
                        profileId, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
                usernames.add(thisusername);
            } catch (UserDoesntFullfillEndEntityProfile e) {
                fail("User " + thisusername + " was not added to the database although it should have been.");
            }
        } finally {
            gc.setEnableEndEntityProfileLimitations(eelimitation);
            globalConfSession.saveConfiguration(roleMgmgToken, gc);            
            endEntityProfileSession.removeEndEntityProfile(admin, eeprofileName);
        }
    }
    
    /**
     * tests creation of new user with unique serialnumber
     * 
     * @throws Exception error
     */
    @Test
    public void test02AddUserWithUniqueDNSerialnumberAndChange() throws Exception {
        log.trace(">test02AddUserWithUniqueDNSerialnumber()");

        // Make user that we know later...
        String thisusername = genRandomUserName();
        String email = thisusername + "@anatom.se";
        genRandomSerialnumber();
        endEntityManagementSession.addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false,
                SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
        assertTrue("User " + thisusername + " was not added to the database.", endEntityManagementSession.existsUser(thisusername));
        usernames.add(thisusername);

        // Set the CA to enforce unique subjectDN serialnumber
        CAInfo cainfo = caSession.getCAInfo(admin, caid);
        boolean requiredUniqueSerialnumber = cainfo.isDoEnforceUniqueSubjectDNSerialnumber();
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(true);
        caAdminSession.editCA(admin, cainfo);

        // Add another user with the same serialnumber
        thisusername = genRandomUserName();
        try {
            endEntityManagementSession.addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email,
                    false, SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0,
                    caid);
            usernames.add(thisusername);
        } catch (EjbcaException e) {
            assertEquals(ErrorCode.SUBJECTDN_SERIALNUMBER_ALREADY_EXISTS, e.getErrorCode());
        }
        assertFalse("Succeeded in adding another end entity with the same serialnumber", endEntityManagementSession.existsUser(thisusername));

        // Set the CA to NOT enforcing unique subjectDN serialnumber
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(false);
        caAdminSession.editCA(admin, cainfo);
        endEntityManagementSession.addUser(admin, thisusername, pwd, "C=SE, CN=" + thisusername + ", SN=" + serialnumber, "rfc822name=" + email, email, false,
                SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, EndEntityTypes.ENDUSER.toEndEntityType(), SecConst.TOKEN_SOFT_P12, 0, caid);
        assertTrue(endEntityManagementSession.existsUser(thisusername));
        usernames.add(thisusername);

        // Set the CA back to its original settings of enforcing unique
        // subjectDN serialnumber.
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(requiredUniqueSerialnumber);
        caAdminSession.editCA(admin, cainfo);

        log.trace("<test02AddUserWithUniqueDNSerialnumber()");
    

        // Make user that we know later...
        String secondUserName;
        if (usernames.size() > 1) {
            secondUserName = (String) usernames.get(1);
        } else {
            secondUserName = username;
        }
        String secondEmail = secondUserName + username + "@anatomanatom.se";

        CAInfo secondCainfo = caSession.getCAInfo(admin, caid);
        boolean secondRequiredUniqueSerialnumber = secondCainfo.isDoEnforceUniqueSubjectDNSerialnumber();

        // Set the CA to enforce unique serialnumber
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(true);
        caAdminSession.editCA(admin, cainfo);    
        try {
            EndEntityInformation user = new EndEntityInformation(secondUserName, "C=SE, CN=" + secondUserName + ", SN=" + serialnumber, caid, "rfc822name=" + secondEmail, secondEmail,
                    new EndEntityType(EndEntityTypes.ENDUSER), SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, null);    
            endEntityManagementSession.changeUser(admin, user, false);
        } catch (EjbcaException e) {
            assertEquals(ErrorCode.SUBJECTDN_SERIALNUMBER_ALREADY_EXISTS, e.getErrorCode());
        }
        assertTrue("The user '" + secondUserName + "' was changed even though the serialnumber already exists.",
                endEntityAccessSession.findUserByEmail(admin, secondEmail).size() == 0);

        // Set the CA to NOT enforcing unique subjectDN serialnumber
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(false);
        caAdminSession.editCA(admin, cainfo);
        EndEntityInformation user = new EndEntityInformation(secondUserName, "C=SE, CN=" + secondUserName + ", SN=" + serialnumber, caid, "rfc822name=" + secondEmail, secondEmail,
                new EndEntityType(EndEntityTypes.ENDUSER), SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, null);    
        endEntityManagementSession.changeUser(admin, user, false);
        assertTrue("The user '" + thisusername + "' was not changed even though unique serialnumber is not enforced", endEntityAccessSession
                .findUserByEmail(admin, secondEmail).size() > 0);

        // Set the CA back to its original settings of enforcing unique
        // subjectDN serialnumber.
        cainfo.setDoEnforceUniqueSubjectDNSerialnumber(secondRequiredUniqueSerialnumber);
        caAdminSession.editCA(admin, secondCainfo);

        log.trace("<test03ChangeUserWithUniqueDNSerialnumber()");

    }

    /**
     * tests findUser and existsUser
     * 
     * @throws Exception error
     */
    @Test
    public void test03FindUser() throws Exception {
        addUser();

        log.trace(">test03FindUser()");
        EndEntityInformation data = endEntityAccessSession.findUser(admin, username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        boolean exists = endEntityManagementSession.existsUser(username);
        assertTrue(exists);

        String notexistusername = genRandomUserName();
        exists = endEntityManagementSession.existsUser(notexistusername);
        assertFalse(exists);
        data = endEntityAccessSession.findUser(admin, notexistusername);
        assertNull(data);
        log.trace("<test03FindUser()");

    }

    /**
     * tests query function
     * 
     * @throws Exception error
     */
    @Test
    public void test03_1QueryUser() throws Exception {
        addUser();

        log.trace(">test03_1QueryUser()");
        Query query = new Query(Query.TYPE_USERQUERY);
        query.add(UserMatch.MATCH_WITH_USERNAME, BasicMatch.MATCH_TYPE_EQUALS, username);
        String caauthstring = null;
        String eeprofilestr = null;
        Collection<EndEntityInformation> col = endEntityManagementProxySession.query(admin, query, caauthstring, eeprofilestr, 0);
        assertNotNull(col);
        assertEquals(1, col.size());
        log.trace("<test03_1QueryUser()");

    }

    /**
     * tests changeUser
     * 
     * @throws Exception error
     */
    @Test
    public void test04ChangeUser() throws Exception {
        addUser();

        log.trace(">test04ChangeUser()");
        EndEntityInformation data = endEntityAccessSession.findUser(admin, username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getCardNumber());
        assertEquals(pwd, data.getPassword()); // Note that changing the user
                                               // sets the password to null!!!
        assertEquals("CN=" + username + ",O=AnaTom,C=SE", data.getDN());
        String email = username + "@anatom.se";
        assertEquals("rfc822name=" + email, data.getSubjectAltName());
        data.setCardNumber("123456");
        data.setPassword("bar123");
        data.setDN("C=SE, O=AnaTom1, CN=" + username);
        data.setSubjectAltName("dnsName=a.b.se, rfc822name=" + email);

        endEntityManagementSession.changeUser(admin, data, true);
        EndEntityInformation data1 = endEntityAccessSession.findUser(admin, username);
        assertNotNull(data1);
        assertEquals(username, data1.getUsername());
        assertEquals("123456", data1.getCardNumber());
        assertEquals("bar123", data1.getPassword());
        assertEquals("CN=" + username + ",O=AnaTom1,C=SE", data1.getDN());
        assertEquals("dnsName=a.b.se, rfc822name=" + email, data1.getSubjectAltName());
        log.trace("<test04ChangeUser()");
    }

    @Test
    public void test05RevokeCert() throws Exception {
        addUser();

        KeyPair keypair = KeyTools.genKeys("512", "RSA");

        EndEntityInformation data1 = endEntityAccessSession.findUser(admin, username);
        assertNotNull(data1);
        data1.setPassword("foo123");
        endEntityManagementSession.changeUser(admin, data1, true);

        Certificate cert = signSession.createCertificate(admin, username, "foo123", new PublicKeyWrapper(keypair.getPublic()));
        CertificateStatus status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.NOT_REVOKED, status.revocationReason);
        // Revoke the certificate, put on hold
        endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert),
                RevokedCertInfo.REVOCATION_REASON_CERTIFICATEHOLD);
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.REVOCATION_REASON_CERTIFICATEHOLD, status.revocationReason);

        // Unrevoke the certificate
        endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert), RevokedCertInfo.NOT_REVOKED);
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.NOT_REVOKED, status.revocationReason);

        // Revoke again certificate
        endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert),
                RevokedCertInfo.REVOCATION_REASON_CERTIFICATEHOLD);
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.REVOCATION_REASON_CERTIFICATEHOLD, status.revocationReason);

        // Unrevoke the certificate, but with different code
        endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert),
                RevokedCertInfo.REVOCATION_REASON_REMOVEFROMCRL);
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.NOT_REVOKED, status.revocationReason);

        // Revoke again certificate permanently
        endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert),
                RevokedCertInfo.REVOCATION_REASON_CACOMPROMISE);
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.REVOCATION_REASON_CACOMPROMISE, status.revocationReason);

        // Unrevoke the certificate, should not work
        try {
            endEntityManagementSession.revokeCert(admin, CertTools.getSerialNumber(cert), CertTools.getIssuerDN(cert),
                    RevokedCertInfo.REVOCATION_REASON_REMOVEFROMCRL);
            assertTrue(false); // should not reach this
        } catch (AlreadyRevokedException e) {
        }
        status = storeSession.getStatus(CertTools.getIssuerDN(cert), CertTools.getSerialNumber(cert));
        assertEquals(RevokedCertInfo.REVOCATION_REASON_CACOMPROMISE, status.revocationReason);
    }

    /**
     * tests deletion of user, and user that does not exist
     * 
     * @throws Exception error
     */
    @Test
    public void test06DeleteUser() throws Exception {
        addUser();

        log.trace(">test06DeleteUser()");
        endEntityManagementSession.deleteUser(admin, username);
        log.debug("deleted user: " + username);
        // Delete the the same user again
        boolean removed = false;
        try {
            endEntityManagementSession.deleteUser(admin, username);
        } catch (NotFoundException e) {
            removed = true;
        }
        assertTrue("User does not exist does not throw NotFoundException", removed);
        log.trace("<test06DeleteUser()");
    }

    /**
     * tests deletion of user, and user that does not exist
     * 
     * @throws Exception error
     */
    @Test
    public void test07MergeWithWS() throws Exception {
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.COMMONNAME);
        profile.addField(DnComponents.DNEMAILADDRESS);
        profile.addField(DnComponents.ORGANIZATIONALUNIT);
        profile.setUse(DnComponents.ORGANIZATIONALUNIT, 0, true);
        profile.setValue(DnComponents.ORGANIZATIONALUNIT, 0, "FooOrgUnit");
        profile.addField(DnComponents.ORGANIZATION);
        profile.addField(DnComponents.COUNTRY);
        profile.setValue(EndEntityProfile.AVAILCAS, 0, Integer.toString(SecConst.ALLCAS));
        profile.setAllowMergeDnWebServices(true);

        endEntityProfileSession.addEndEntityProfile(admin, "TESTMERGEWITHWS", profile);
        int profileId = endEntityProfileSession.getEndEntityProfileId("TESTMERGEWITHWS");

        EndEntityInformation addUser = new EndEntityInformation(username, "C=SE, O=AnaTom, CN=" + username, caid, null, null,
                EndEntityConstants.STATUS_NEW, new EndEntityType(EndEntityTypes.ENDUSER), profileId, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, new Date(), new Date(),
                SecConst.TOKEN_SOFT_P12, 0, null);
        addUser.setPassword("foo123");
        endEntityManagementSession.addUserFromWS(admin, addUser, false);
        EndEntityInformation data = endEntityAccessSession.findUser(admin, username);
        assertEquals("CN=" + username + ",OU=FooOrgUnit,O=AnaTom,C=SE", data.getDN());

        addUser.setDN("EMAIL=foo@bar.com, OU=hoho");
        endEntityProfileSession.changeEndEntityProfile(admin, "TESTMERGEWITHWS", profile);
        endEntityManagementSession.changeUser(admin, addUser, false, true);
        data = endEntityAccessSession.findUser(admin, username);
        // E=foo@bar.com,CN=430208,OU=FooOrgUnit,O=hoho,C=NO
        assertEquals("E=foo@bar.com,CN=" + username + ",OU=hoho,O=AnaTom,C=SE", data.getDN());
    }
    
    /** Tests that CA and End Entity profile authorization methods in EndEntityManagementSessionBean works.
     * When called with a user that does not have access to the CA (that you try to add a user for), or the
     * end entity profile specified for the user, an AuthorizationDeniedException should be thrown.
     * For end entity profile authorization to be effective, this must be configured in global configuration.
     */
    @Test
    public void test08Authorization() throws Exception {
        
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(new X500Principal("C=SE,O=Test,CN=Test EndEntityManagementSessionNoAuth"));
        
        TestX509CertificateAuthenticationToken adminTokenNoAuth  = (TestX509CertificateAuthenticationToken) simpleAuthenticationProvider.authenticate(new AuthenticationSubject(principals, null));
        final X509Certificate adminCert = adminTokenNoAuth.getCertificate();

        final String testRole = "EndEntityManagementSessionTestAuthRole";
        GlobalConfiguration gc = (GlobalConfiguration) globalConfSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
        boolean eelimitation = gc.getEnableEndEntityProfileLimitations();

        final String authUsername = genRandomUserName();
        String email = authUsername + "@anatom.se";
        EndEntityInformation userdata = new EndEntityInformation(authUsername, "C=SE, O=AnaTom, CN=" + username, caid, null, email, new EndEntityType(EndEntityTypes.ENDUSER), SecConst.EMPTY_ENDENTITYPROFILE, CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, null);
        userdata.setPassword("foo123");
        // Test CA authorization
        try {
            try {
                endEntityManagementSession.addUser(adminTokenNoAuth, userdata, false);
                fail("should throw");
            } catch (AuthorizationDeniedException e) {
                assertTrue("Wrong auth denied message: "+e.getMessage(), StringUtils.startsWith(e.getMessage(), "Administrator not authorized to CA"));
            }

            try {
                endEntityManagementSession.changeUser(adminTokenNoAuth, userdata, true);
                fail("should throw");
            } catch (AuthorizationDeniedException e) {
                assertTrue("Wrong auth denied message: "+e.getMessage(), StringUtils.startsWith(e.getMessage(), "Administrator not authorized to CA"));
            }

            try {
                endEntityManagementSession.addUser(admin, userdata, false);
                endEntityManagementSession.deleteUser(adminTokenNoAuth, authUsername);
                fail("should throw");
            } catch (AuthorizationDeniedException e) {
                assertTrue("Wrong auth denied message: "+e.getMessage(), StringUtils.startsWith(e.getMessage(), "Administrator not authorized to CA"));
            }
            
            // Now add the administrator to a role that has access to /ca/* but not ee profiles
            RoleData role = roleAccessSession.findRole(testRole);
            if (role == null) {
                role = roleManagementSession.create(roleMgmgToken, testRole);
            }
            final List<AccessRuleData> accessRules = new ArrayList<AccessRuleData>();
            accessRules.add(new AccessRuleData(testRole, StandardRules.CAACCESSBASE.resource(), AccessRuleState.RULE_ACCEPT, true));
            role = roleManagementSession.addAccessRulesToRole(roleMgmgToken, role, accessRules);

            final List<AccessUserAspectData> accessUsers = new ArrayList<AccessUserAspectData>();
            accessUsers.add(new AccessUserAspectData(testRole, CertTools.getIssuerDN(adminCert).hashCode(), X500PrincipalAccessMatchValue.WITH_COMMONNAME,
                    AccessMatchType.TYPE_EQUALCASE, CertTools.getPartFromDN(CertTools.getSubjectDN(adminCert), "CN")));
            roleManagementSession.addSubjectsToRole(roleMgmgToken, role, accessUsers);
            accessControlSession.forceCacheExpire();
            // We must enforce end entity profile limitations for this, with false it should be ok now
            gc.setEnableEndEntityProfileLimitations(false);
            globalConfSession.saveConfiguration(roleMgmgToken, gc);
            // Do the same test, now it should work since we are authorized to CA and we don't enforce EE profile authorization
            endEntityManagementSession.changeUser(adminTokenNoAuth, userdata, false);
            // Enforce EE profile limitations
            gc.setEnableEndEntityProfileLimitations(true);
            globalConfSession.saveConfiguration(roleMgmgToken, gc);
            // Do the same test, now we should get auth denied on EE profiles instead
            try {
                endEntityManagementSession.changeUser(adminTokenNoAuth, userdata, false);
                fail("should throw");
            } catch (AuthorizationDeniedException e) {
                assertTrue("Wrong auth denied message: "+e.getMessage(), StringUtils.startsWith(e.getMessage(), "Administrator not authorized to end entity profile"));
            }

        } finally {
            gc.setEnableEndEntityProfileLimitations(eelimitation);
            globalConfSession.saveConfiguration(roleMgmgToken, gc);
            try {
                endEntityManagementSession.deleteUser(admin, authUsername);
            } catch (Exception e) { // NOPMD
                log.info("Error in finally: ", e);
            }
            RoleData role = roleAccessSession.findRole(testRole);
            if (role != null) {
                roleManagementSession.remove(roleMgmgToken, role);
            }
        }
    }


}
