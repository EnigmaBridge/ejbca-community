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

package org.ejbca.core.ejb.keyrecovery;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.FinderException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificate.CertificateInfo;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.keys.util.KeyPairWrapper;
import org.cesecore.util.CertTools;
import org.ejbca.core.ejb.approval.ApprovalSessionLocal;
import org.ejbca.core.ejb.audit.enums.EjbcaEventTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaModuleTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaServiceTypes;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalExecutorUtil;
import org.ejbca.core.model.approval.ApprovalOveradableClassName;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.approval.approvalrequests.KeyRecoveryApprovalRequest;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.KeyRecoveryCAServiceRequest;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.KeyRecoveryCAServiceResponse;
import org.ejbca.core.model.keyrecovery.KeyRecoveryInformation;

/**
 * Stores key recovery data.
 *
 * @version $Id: KeyRecoverySessionBean.java 20728 2015-02-20 14:55:55Z mikekushner $
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "KeyRecoverySessionRemote")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class KeyRecoverySessionBean implements KeyRecoverySessionLocal, KeyRecoverySessionRemote {

    private static final Logger log = Logger.getLogger(KeyRecoverySessionBean.class);
    
    /** Internal localization of logs and errors */
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();
    
    @PersistenceContext(unitName="ejbca")
    private EntityManager entityManager;

    @EJB
    private AccessControlSessionLocal authorizationSession;
    @EJB
    private ApprovalSessionLocal approvalSession;  
    @EJB
    private CAAdminSessionLocal caAdminSession;
    @EJB
    private CertificateStoreSessionLocal certificateStoreSession;
    @EJB
    private SecurityEventsLoggerSessionLocal auditSession;
	
	    /**
     * Method checking the following authorizations:
     * 
     * If /superadmin -> true
     * 
     * Other must have both
     * AccessRulesConstants.
     *  /ra_functionality/keyrecovery
     *  and /endentityprofilesrules/<endentityprofile>/keyrecovery
     *  
     * 
     * @param admin
     * @param profileid end entity profile
     * @return true if the admin is authorized to keyrecover
     */
    private boolean authorizedToKeyRecover(AuthenticationToken admin, int profileid) throws AuthorizationDeniedException {
        return authorizationSession.isAuthorizedNoLogging(admin, AccessRulesConstants.ENDENTITYPROFILEPREFIX + profileid
                + AccessRulesConstants.KEYRECOVERY_RIGHTS)
                && authorizationSession.isAuthorizedNoLogging(admin, AccessRulesConstants.REGULAR_KEYRECOVERY);

    }
    
    /**
     * 
     * @param token The {@link AuthenticationToken} to check. 
     * @return true if authorized to or /ra_functionality/keyrecovery
     */
    private boolean authorizedToAdministrateKeys(AuthenticationToken token) {
        return authorizationSession.isAuthorizedNoLogging(token, AccessRulesConstants.REGULAR_KEYRECOVERY);
    }

    /**
     * Help method to check if approval of key recovery is required
     * @param admin 
     * @param certificate 
     * @param username 
     * @param userdata 
     * @param checkNewest 
     * @throws ApprovalException 
     * @throws WaitingForApprovalException 
     */
    private void checkIfApprovalRequired(AuthenticationToken admin, Certificate certificate, String username, int endEntityProfileId, boolean checkNewest) throws ApprovalException, WaitingForApprovalException{    	
        final int caid = CertTools.getIssuerDN(certificate).hashCode();
		final CertificateInfo certinfo = certificateStoreSession.getCertificateInfo(CertTools.getFingerprintAsString(certificate));
        // Check if approvals is required.
        int numOfApprovalsRequired = caAdminSession.getNumOfApprovalRequired(CAInfo.REQ_APPROVAL_KEYRECOVER, caid, certinfo.getCertificateProfileId());
        if (numOfApprovalsRequired > 0){    
			KeyRecoveryApprovalRequest ar = new KeyRecoveryApprovalRequest(certificate,username,checkNewest, admin,null,numOfApprovalsRequired,caid,endEntityProfileId);
			if (ApprovalExecutorUtil.requireApproval(ar, NONAPPROVABLECLASSNAMES_KEYRECOVERY)){
				approvalSession.addApprovalRequest(admin, ar);
	            String msg = intres.getLocalizedMessage("keyrecovery.addedforapproval");            	
				throw new WaitingForApprovalException(msg, ar.generateApprovalId());
			}
        } 
    }
    
    @Override
    public boolean addKeyRecoveryData(AuthenticationToken admin, Certificate certificate, String username, KeyPairWrapper keypair)
            throws AuthorizationDeniedException {
  	if (log.isTraceEnabled()) {
            log.trace(">addKeyRecoveryData(user: " + username + ")");
    	}
        if (authorizedToAdministrateKeys(admin)) {
            final int caid = CertTools.getIssuerDN(certificate).hashCode();
            final String certSerialNumber = CertTools.getSerialNumberAsString(certificate);
            boolean returnval = false;
            try {
                KeyRecoveryCAServiceResponse response = (KeyRecoveryCAServiceResponse) caAdminSession.extendedService(admin, caid,
                        new KeyRecoveryCAServiceRequest(KeyRecoveryCAServiceRequest.COMMAND_ENCRYPTKEYS, keypair.getKeyPair()));
                entityManager.persist(new org.ejbca.core.ejb.keyrecovery.KeyRecoveryData(CertTools.getSerialNumber(certificate), CertTools
                        .getIssuerDN(certificate), username, response.getKeyData(), response.getCryptoTokenId(), response.getKeyAlias(), response.getPublicKeyId()));
                // same method to make hex serno as in KeyRecoveryDataBean
                String msg = intres.getLocalizedMessage("keyrecovery.addeddata", CertTools.getSerialNumber(certificate).toString(16),
                        CertTools.getIssuerDN(certificate), response.getKeyAlias(), response.getPublicKeyId(), response.getCryptoTokenId());
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_ADDDATA, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA,
                        admin.toString(), String.valueOf(caid), certSerialNumber, username, details);
                returnval = true;
            } catch (Exception e) {
                final String msg = intres.getLocalizedMessage("keyrecovery.erroradddata", CertTools.getSerialNumber(certificate).toString(16),
                        CertTools.getIssuerDN(certificate));
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_ADDDATA, EventStatus.FAILURE, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA,
                        admin.toString(), String.valueOf(caid), certSerialNumber, username, details);
                log.error(msg, e);
            }
            log.trace("<addKeyRecoveryData()");
            return returnval;
        } else {
            throw new AuthorizationDeniedException(admin + " not authorized to administer keys");
        }
        
    }

    @Override
    public boolean changeKeyRecoveryData(AuthenticationToken admin, X509Certificate certificate, boolean markedasrecoverable, KeyPairWrapper keypair) throws AuthorizationDeniedException {
    	if (log.isTraceEnabled()) {
            log.trace(">changeKeyRecoveryData(certsn: " + certificate.getSerialNumber().toString(16) + ", " +
                    CertTools.getIssuerDN(certificate) + ")");
    	}
    	if(authorizedToAdministrateKeys(admin)) {
    	    boolean returnval = false;
    	    final String hexSerial = certificate.getSerialNumber().toString(16);
    	    final String dn = CertTools.getIssuerDN(certificate);
    	    final int caid = dn.hashCode();
    	    try {
    	        final KeyRecoveryData krd = KeyRecoveryData.findByPK(entityManager, new KeyRecoveryDataPK(hexSerial, dn));
    	        if (krd == null) {
    	            throw new FinderException();
    	        }
    	        krd.setMarkedAsRecoverable(markedasrecoverable);
    	        final KeyRecoveryCAServiceResponse response = (KeyRecoveryCAServiceResponse) caAdminSession.extendedService(admin, caid,
    	                new KeyRecoveryCAServiceRequest(KeyRecoveryCAServiceRequest.COMMAND_ENCRYPTKEYS, keypair.getKeyPair()));
    	        krd.setKeyDataFromByteArray(response.getKeyData());
    	        // Update with the key information for the key used to protect this new key recovery data
    	        krd.setCryptoTokenId(response.getCryptoTokenId());
    	        krd.setKeyAlias(response.getKeyAlias());
    	        krd.setPublicKeyId(response.getPublicKeyId());
    	        final String msg = intres.getLocalizedMessage("keyrecovery.changeddata", hexSerial, dn, response.getKeyAlias(), response.getPublicKeyId(), response.getCryptoTokenId());            	
    	        final Map<String, Object> details = new LinkedHashMap<String, Object>();
    	        details.put("msg", msg);
    	        auditSession.log(EjbcaEventTypes.KEYRECOVERY_EDITDATA, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), String.valueOf(caid), hexSerial, krd.getUsername(), details);
    	        returnval = true;
    	    } catch (Exception e) {
    	        final String msg = intres.getLocalizedMessage("keyrecovery.errorchangedata", hexSerial, dn);            	
    	        final Map<String, Object> details = new LinkedHashMap<String, Object>();
    	        details.put("msg", msg);
    	        auditSession.log(EjbcaEventTypes.KEYRECOVERY_EDITDATA, EventStatus.FAILURE, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), String.valueOf(caid), hexSerial, null, details);
    	        log.error(msg, e);
    	    }
    	    if (log.isTraceEnabled()) {
    	        log.trace("<changeKeyRecoveryData()");
    	    }
    	    return returnval;
    	} else {
    	    throw new AuthorizationDeniedException(admin + " not authorized to administer key recovery keys");
    	}
    }

    @Override
    public void removeKeyRecoveryData(AuthenticationToken admin, Certificate certificate) throws AuthorizationDeniedException {
        if(authorizedToAdministrateKeys(admin)) {
        final String hexSerial = CertTools.getSerialNumber(certificate).toString(16);
    	if (log.isTraceEnabled()) {
            log.trace(">removeKeyRecoveryData(certificate: " + CertTools.getSerialNumber(certificate).toString(16) +")");
    	}
        final String dn = CertTools.getIssuerDN(certificate);
        final int caid = dn.hashCode();
        try {
            String username = null;
        	org.ejbca.core.ejb.keyrecovery.KeyRecoveryData krd = org.ejbca.core.ejb.keyrecovery.KeyRecoveryData.findByPK(entityManager, new KeyRecoveryDataPK(hexSerial, dn));
        	if (krd == null) {
        		throw new FinderException();
        	}
            username = krd.getUsername();
            entityManager.remove(krd);
            String msg = intres.getLocalizedMessage("keyrecovery.removeddata", hexSerial, dn);            	
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.KEYRECOVERY_REMOVEDATA, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), String.valueOf(caid), hexSerial, username, details);
        } catch (Exception e) {
            final String msg = intres.getLocalizedMessage("keyrecovery.errorremovedata", hexSerial, dn);            	
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.KEYRECOVERY_REMOVEDATA, EventStatus.FAILURE, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), String.valueOf(caid), hexSerial, null, details);
            log.error(msg, e);
        }
        log.trace("<removeKeyRecoveryData()");
        } else {
            throw new AuthorizationDeniedException(admin + " not authorized to administer keys");
        }
    }

    @Override
    public void removeAllKeyRecoveryData(AuthenticationToken admin, String username) {
    	if (log.isTraceEnabled()) {
            log.trace(">removeAllKeyRecoveryData(user: " + username + ")");
    	}
        try {
        	Collection<org.ejbca.core.ejb.keyrecovery.KeyRecoveryData> result = org.ejbca.core.ejb.keyrecovery.KeyRecoveryData.findByUsername(entityManager, username);
            Iterator<org.ejbca.core.ejb.keyrecovery.KeyRecoveryData> iter = result.iterator();
            while (iter.hasNext()) {
            	entityManager.remove(iter.next());
            }
            String msg = intres.getLocalizedMessage("keyrecovery.removeduser", username);            	
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.KEYRECOVERY_REMOVEDATA, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), null, null, username, details);
        } catch (Exception e) {
            String msg = intres.getLocalizedMessage("keyrecovery.errorremoveuser", username);            	
            final Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("msg", msg);
            auditSession.log(EjbcaEventTypes.KEYRECOVERY_REMOVEDATA, EventStatus.FAILURE, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), null, null, username, details);
        }
        log.trace("<removeAllKeyRecoveryData()");
    }

    @Override
    public KeyRecoveryInformation keyRecovery(AuthenticationToken admin, String username, int endEntityProfileId) throws AuthorizationDeniedException {
        return recoverKeys(admin, username, endEntityProfileId);
    }
    
    @Override
    public KeyRecoveryInformation recoverKeys(AuthenticationToken admin, String username, int endEntityProfileId) throws AuthorizationDeniedException {
    	if (log.isTraceEnabled()) {
            log.trace(">keyRecovery(user: " + username + ")");
    	}
        KeyRecoveryInformation returnval = null;
        KeyRecoveryData krd = null;
        X509Certificate certificate = null;
        if (authorizedToKeyRecover(admin, endEntityProfileId)) { 
        	Collection<KeyRecoveryData> result = KeyRecoveryData.findByUserMark(entityManager, username);
        	Iterator<KeyRecoveryData> i = result.iterator();
        	try {
        		String caidString = null;
        		String certSerialNumber = null;
        		String logMsg = null;
        		while (i.hasNext()) {
        			krd = i.next();
        			if (returnval == null) {
        				final int caid = krd.getIssuerDN().hashCode();
        				caidString = String.valueOf(caid);
        				final KeyRecoveryCAServiceResponse response = (KeyRecoveryCAServiceResponse) caAdminSession.extendedService(admin, caid,
        						new KeyRecoveryCAServiceRequest(KeyRecoveryCAServiceRequest.COMMAND_DECRYPTKEYS, krd.getKeyDataAsByteArray(),
        						        krd.getCryptoTokenId(), krd.getKeyAlias()));
        				final KeyPair keys = response.getKeyPair();
        				certificate = (X509Certificate) certificateStoreSession.findCertificateByIssuerAndSerno(krd.getIssuerDN(), krd.getCertificateSN());
        				returnval = new KeyRecoveryInformation(krd.getCertificateSN(), krd.getIssuerDN(),
        						krd.getUsername(), krd.getMarkedAsRecoverable(), keys, certificate);
                		certSerialNumber = CertTools.getSerialNumberAsString(certificate);
                        logMsg = intres.getLocalizedMessage("keyrecovery.sentdata", username, response.getKeyAlias(), response.getPublicKeyId(), response.getCryptoTokenId());                
        			}
        		}
        		if (logMsg == null) {
                    logMsg = intres.getLocalizedMessage("keyrecovery.nodata", username);                        		    
        		}
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", logMsg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_SENT, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), caidString, certSerialNumber, username, details);
        	} catch (Exception e) {
        		String msg = intres.getLocalizedMessage("keyrecovery.errorsenddata", username);            	
        		log.error(msg, e);
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_SENT, EventStatus.FAILURE, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), null, null, username, details);
        	}
        } else {
            throw new AuthorizationDeniedException(admin + " not authorized to key recovery for end entity profile id " + endEntityProfileId);
        }
        if (log.isTraceEnabled()) {
            log.trace("<keyRecovery()");
        }
        return returnval;
    }

	private static final ApprovalOveradableClassName[] NONAPPROVABLECLASSNAMES_KEYRECOVERY = {
		new ApprovalOveradableClassName(org.ejbca.core.model.approval.approvalrequests.KeyRecoveryApprovalRequest.class.getName(),null),		
	};
	
	@Override
    public boolean markNewestAsRecoverable(AuthenticationToken admin, String username, int endEntityProfileId) throws AuthorizationDeniedException, ApprovalException, WaitingForApprovalException {
    	if (log.isTraceEnabled()) {
            log.trace(">markNewestAsRecoverable(user: " + username + ")");
    	}
        boolean returnval = false;
        long newesttime = 0;
        org.ejbca.core.ejb.keyrecovery.KeyRecoveryData krd = null;
        org.ejbca.core.ejb.keyrecovery.KeyRecoveryData newest = null;
        X509Certificate certificate = null;
        X509Certificate newestcertificate = null;
        if (!isUserMarked(username)) {
            String caidString = null;
            String certSerialNumber = null;
        	Collection<org.ejbca.core.ejb.keyrecovery.KeyRecoveryData> result = org.ejbca.core.ejb.keyrecovery.KeyRecoveryData.findByUsername(entityManager, username);
        	Iterator<org.ejbca.core.ejb.keyrecovery.KeyRecoveryData> iter = result.iterator();
        	while (iter.hasNext()) {
        		krd = iter.next();
        		caidString = String.valueOf(krd.getIssuerDN().hashCode());
        		certificate = (X509Certificate) certificateStoreSession.findCertificateByIssuerAndSerno(krd.getIssuerDN(), krd.getCertificateSN());
        		if (certificate != null) {
        			if (certificate.getNotBefore().getTime() > newesttime) {
        				newesttime = certificate.getNotBefore().getTime();
        				newest = krd;
        				newestcertificate = certificate;
                		certSerialNumber = CertTools.getSerialNumberAsString(newestcertificate);
        			}
        		}
        	}
        	if (newest != null) {
        		// Check that the administrator is authorized to keyrecover
                if (authorizedToKeyRecover(admin, endEntityProfileId)) {
                    // Check if approvals is required.            
                    checkIfApprovalRequired(admin, newestcertificate, username, endEntityProfileId, true);
                    newest.setMarkedAsRecoverable(true);
                    returnval = true;
                } else {
                    throw new AuthorizationDeniedException(admin + " not authorized to key recovery for end entity profile id " + endEntityProfileId);
                }
        	}
        	if (returnval) {
        		String msg = intres.getLocalizedMessage("keyrecovery.markeduser", username);            	
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_MARKED, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA, admin.toString(), caidString, certSerialNumber, username, details);
        	} else {
        		String msg = intres.getLocalizedMessage("keyrecovery.errormarkuser", username);
        		log.info(msg);
        	}
        }
        log.trace("<markNewestAsRecoverable()");
        return returnval;
    }

	@Override
    public boolean markAsRecoverable(AuthenticationToken admin, Certificate certificate, int endEntityProfileId) throws AuthorizationDeniedException, WaitingForApprovalException, ApprovalException {        
        final String hexSerial = CertTools.getSerialNumber(certificate).toString(16); // same method to make hex as in KeyRecoveryDataBean
        final String dn = CertTools.getIssuerDN(certificate);        
    	if (log.isTraceEnabled()) {
            log.trace(">markAsRecoverable(issuer: "+dn+"; certificatesn: " + hexSerial + ")");
    	}
        boolean returnval = false;
    	org.ejbca.core.ejb.keyrecovery.KeyRecoveryData krd = org.ejbca.core.ejb.keyrecovery.KeyRecoveryData.findByPK(entityManager, new KeyRecoveryDataPK(hexSerial, dn));
        if (krd != null) {
            String username = krd.getUsername();
            // Check that the administrator is authorized to keyrecover
            if (authorizedToKeyRecover(admin, endEntityProfileId)) {
                // Check if approvals is required.            
                checkIfApprovalRequired(admin, certificate, username, endEntityProfileId, false);
                krd.setMarkedAsRecoverable(true);
                int caid = krd.getIssuerDN().hashCode();
                String msg = intres.getLocalizedMessage("keyrecovery.markedcert", hexSerial, dn);
                final Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("msg", msg);
                auditSession.log(EjbcaEventTypes.KEYRECOVERY_MARKED, EventStatus.SUCCESS, EjbcaModuleTypes.KEYRECOVERY, EjbcaServiceTypes.EJBCA,
                        admin.toString(), String.valueOf(caid), hexSerial, username, details);
                returnval = true;
            } else {
                throw new AuthorizationDeniedException(admin + " not authorized to key recovery for end entity profile id " + endEntityProfileId);
            }
    	} else {
            String msg = intres.getLocalizedMessage("keyrecovery.errormarkcert", hexSerial, dn);            	
        	log.info(msg);
        } 
        log.trace("<markAsRecoverable()");
        return returnval;
    }

	@Override
    public void unmarkUser(AuthenticationToken admin, String username) {
    	if (log.isTraceEnabled()) {
            log.trace(">unmarkUser(user: " + username + ")");
    	}
    	KeyRecoveryData krd = null;
    	Collection<KeyRecoveryData> result = KeyRecoveryData.findByUserMark(entityManager, username);
    	Iterator<KeyRecoveryData> i = result.iterator();
    	while (i.hasNext()) {
    		krd = i.next();
    		krd.setMarkedAsRecoverable(false);
    	}
        log.trace("<unmarkUser()");
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public boolean isUserMarked(String username) {
    	if (log.isTraceEnabled()) {
            log.trace(">isUserMarked(user: " + username + ")");
    	}
        boolean returnval = false;       
        Collection<KeyRecoveryData> result = KeyRecoveryData.findByUserMark(entityManager, username);
        for(KeyRecoveryData krd : result) {
        	if (krd.getMarkedAsRecoverable()) {
        		returnval = true;
        		break;
        	}
        }
    	if (log.isTraceEnabled()) {
            log.trace("<isUserMarked(" + returnval + ")");
    	}
        return returnval;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public boolean existsKeys(Certificate certificate) {
        log.trace(">existsKeys()");
        boolean returnval = false;
        final String hexSerial = CertTools.getSerialNumber(certificate).toString(16); // same method to make hex as in KeyRecoveryDataBean
        final String dn = CertTools.getIssuerDN(certificate);
    	KeyRecoveryData krd = KeyRecoveryData.findByPK(entityManager, new KeyRecoveryDataPK(hexSerial, dn));
    	if (krd != null) {
            log.debug("Found key for user: "+krd.getUsername());
            returnval = true;
        }
    	if (log.isTraceEnabled()) {
            log.trace("<existsKeys(" + returnval + ")");
    	}
        return returnval;
    }
}
