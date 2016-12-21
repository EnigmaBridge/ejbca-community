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

package org.ejbca.core.ejb;

import javax.ejb.Local;

import org.cesecore.audit.audit.SecurityEventsAuditorSessionLocal;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.rules.AccessRuleManagementSessionLocal;
import org.cesecore.authorization.user.AccessUserAspectManagerSessionLocal;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificate.CertificateCreateSessionLocal;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.certificates.crl.CrlCreateSessionLocal;
import org.cesecore.certificates.crl.CrlStoreSessionLocal;
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.keybind.InternalKeyBindingMgmtSessionLocal;
import org.cesecore.keys.token.CryptoTokenManagementSessionLocal;
import org.cesecore.roles.access.RoleAccessSessionLocal;
import org.cesecore.roles.management.RoleManagementSessionLocal;
import org.ejbca.core.ejb.approval.ApprovalExecutionSessionLocal;
import org.ejbca.core.ejb.approval.ApprovalSessionLocal;
import org.ejbca.core.ejb.audit.EjbcaAuditorSessionLocal;
import org.ejbca.core.ejb.authentication.web.WebAuthenticationProviderSessionLocal;
import org.ejbca.core.ejb.authorization.ComplexAccessControlSessionLocal;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionLocal;
import org.ejbca.core.ejb.ca.publisher.PublisherQueueSessionLocal;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionLocal;
import org.ejbca.core.ejb.ca.revoke.RevocationSessionLocal;
import org.ejbca.core.ejb.ca.sign.SignSessionLocal;
import org.ejbca.core.ejb.ca.store.CertReqHistorySessionLocal;
import org.ejbca.core.ejb.crl.PublishingCrlSessionLocal;
import org.ejbca.core.ejb.hardtoken.HardTokenBatchJobSessionLocal;
import org.ejbca.core.ejb.hardtoken.HardTokenSessionLocal;
import org.ejbca.core.ejb.keyrecovery.KeyRecoverySessionLocal;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.AdminPreferenceSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.ejb.ra.userdatasource.UserDataSourceSessionLocal;
import org.ejbca.core.ejb.services.ServiceSessionLocal;
import org.ejbca.core.protocol.cmp.CmpMessageDispatcherSessionLocal;

/**
 * Due to the lack of standardization in JEE5 there is no way to lookup local interfaces.
 * 
 * This Stateless Session Bean (SSB) act as a bridge between calling classes in the same JVM,
 * and the real ejb references.
 * 
 * This will allow us to define a single (this) local EJB in all web.xml and ejb-jar.xml files
 * and are then free to change and move around SSBs and their interfaces without XML changes.
 * 
 * @version $Id: EjbBridgeSessionLocal.java 19968 2014-10-09 13:13:58Z mikekushner $
 */
@Local
public interface EjbBridgeSessionLocal {

	AccessControlSessionLocal getAccessControlSession();
	AccessRuleManagementSessionLocal getAccessRuleManagementSession();
	AccessUserAspectManagerSessionLocal getAccessUserAspectSession();
	ApprovalExecutionSessionLocal getApprovalExecutionSession();
	ApprovalSessionLocal getApprovalSession();
	CAAdminSessionLocal getCaAdminSession();
	CaSessionLocal getCaSession();
	CertificateProfileSessionLocal getCertificateProfileSession();
	CertificateStoreSessionLocal getCertificateStoreSession();
	CertReqHistorySessionLocal getCertReqHistorySession();
	ComplexAccessControlSessionLocal getComplexAccessControlSession();
	RevocationSessionLocal getRevocationSession();
	CmpMessageDispatcherSessionLocal getCmpMessageDispatcherSession();
	CrlStoreSessionLocal getCrlStoreSession();
	CrlCreateSessionLocal getCrlCreateSession();
    EjbcaAuditorSessionLocal getEjbcaAuditorSession();
	EndEntityAuthenticationSessionLocal getEndEntityAuthenticationSession();
	EndEntityAccessSessionLocal getEndEntityAccessSession();
	EndEntityProfileSessionLocal getEndEntityProfileSession();
	GlobalConfigurationSessionLocal getGlobalConfigurationSession();
	HardTokenBatchJobSessionLocal getHardTokenBatchJobSession();
	HardTokenSessionLocal getHardTokenSession();
	KeyRecoverySessionLocal getKeyRecoverySession();
	PublisherQueueSessionLocal getPublisherQueueSession();
	PublisherSessionLocal getPublisherSession();
	AdminPreferenceSessionLocal getRaAdminSession();
	RoleAccessSessionLocal getRoleAccessSession();
	RoleManagementSessionLocal getRoleManagementSession();
	SecurityEventsAuditorSessionLocal getSecurityEventsAuditorSession();
	SecurityEventsLoggerSessionLocal getSecurityEventsLoggerSession();
	ServiceSessionLocal getServiceSession();
	SignSessionLocal getSignSession();
	CertificateCreateSessionLocal getCertificateCreateSession();
	UserDataSourceSessionLocal getUserDataSourceSession();
	EndEntityManagementSessionLocal getEndEntityManagementSession();
	WebAuthenticationProviderSessionLocal getWebAuthenticationProviderSession();
    CryptoTokenManagementSessionLocal getCryptoTokenManagementSession();
    InternalKeyBindingMgmtSessionLocal getInternalKeyBindingMgmtSession();
    PublishingCrlSessionLocal getPublishingCrlSession();
}
