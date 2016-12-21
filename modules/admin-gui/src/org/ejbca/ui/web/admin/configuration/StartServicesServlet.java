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
 
package org.ejbca.ui.web.admin.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.FinderException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.audit.AuditDevicesConfig;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.enums.EventTypes;
import org.cesecore.audit.enums.ModuleTypes;
import org.cesecore.audit.enums.ServiceTypes;
import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.user.matchvalues.X500PrincipalAccessMatchValue;
import org.cesecore.certificates.certificate.CertificateCreateSessionLocal;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.ocsp.OcspResponseGeneratorSessionLocal;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.config.GlobalOcspConfiguration;
import org.cesecore.config.OcspConfiguration;
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.keys.token.CryptoTokenFactory;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.config.EjbcaConfiguration;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.EnterpriseEditionEjbBridgeSessionLocal;
import org.ejbca.core.ejb.audit.enums.EjbcaEventTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaModuleTypes;
import org.ejbca.core.ejb.audit.enums.EjbcaServiceTypes;
import org.ejbca.core.ejb.authentication.cli.CliUserAccessMatchValue;
import org.ejbca.core.ejb.authorization.ComplexAccessControlSessionLocal;
import org.ejbca.core.ejb.ca.caadmin.CAAdminSessionLocal;
import org.ejbca.core.ejb.ca.publisher.PublisherSessionLocal;
import org.ejbca.core.ejb.ocsp.OcspKeyRenewalSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.ejb.services.ServiceSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;

/**
 * Servlet used to start services by calling the ServiceSession.load() at startup<br>
 * 
 * @version $Id: StartServicesServlet.java 21141 2015-04-27 11:27:26Z mikekushner $
 */
public class StartServicesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(StartServicesServlet.class);
    /** Internal localization of logs and errors */
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();
    
    @EJB
    private CAAdminSessionLocal caAdminSession;
    @EJB
    private CertificateCreateSessionLocal certCreateSession;
    @EJB
    private CertificateProfileSessionLocal certificateProfileSession;
    @EJB
    private CertificateStoreSessionLocal certificateStoreSession;
    @EJB
    private ComplexAccessControlSessionLocal complexAccessControlSession;
    @EJB
    private EndEntityAccessSessionLocal endEntityAccessSession;
    @EJB
    private EndEntityManagementSessionLocal endEntityManagementSession;
    @EJB
    private EndEntityProfileSessionLocal endEntityProfileSession;
    @EJB
    private EnterpriseEditionEjbBridgeSessionLocal enterpriseEditionEjbBridgeSession;
    @EJB
    private GlobalConfigurationSessionLocal globalConfigurationSession;
    @EJB
    private OcspKeyRenewalSessionLocal ocspKeyRenewalSession;
    @EJB
    private OcspResponseGeneratorSessionLocal ocspResponseGeneratorSession;
    @EJB
    private PublisherSessionLocal publisherSession;
    @EJB
    private ServiceSessionLocal serviceSession;
    @EJB
    private SecurityEventsLoggerSessionLocal logSession;

    @Resource
    private UserTransaction tx;
    
    /**
     * Method used to log service shutdown.
	 * @see javax.servlet.GenericServlet#destroy()
	 **/
    @Override
	public void destroy() {
        String iMsg = intres.getLocalizedMessage("startservice.shutdown");
        log.info(iMsg);

        // Making an EJB call to audit log shutdown is not possible, since the EJBs will shut down before the servlet. 
        // This should be possible to handle  with EJB 3.1
        /*
        @Singleton
        @Startup
        public class StartupBean {

        @PostConstruct
        private void startup() { ... }

        @PreDestroy
        private void shutdown() { ... }
        ...
        }
        */
        // Make a log row that EJBCA is stopping
//        final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("StartServicesServlet.destroy"));
//        final Map<String, Object> details = new LinkedHashMap<String, Object>();
//        details.put("msg", iMsg);
//        logSession.log(EjbcaEventTypes.EJBCA_STOPPING, EventStatus.SUCCESS, EjbcaModuleTypes.SERVICE, EjbcaServiceTypes.EJBCA, admin.toString(), null, null, null, details);                
        super.destroy();
	}

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ejbcaInit();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
        log.trace(">doPost()");
        doGet(req, res);
        log.trace("<doPost()");
    } //doPost

    @Override
    public void doGet(HttpServletRequest req,  HttpServletResponse res) throws java.io.IOException, ServletException {
        log.trace(">doGet()");
    	res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Servlet doesn't support requests is only loaded on startup.");
        log.trace("<doGet()");
    } // doGet

    @SuppressWarnings("deprecation")
    private void ejbcaInit() {
    	
        //
        // Run all "safe" initializations first, 
        // i.e. those that does not depend on other running beans, components etc

        
        // Log a startup message
        String iMsg = intres.getLocalizedMessage("startservice.startup", GlobalConfiguration.EJBCA_VERSION);
        log.info(iMsg);

        // Reinstall BC-provider to help re-deploys to work
        log.trace(">init re-installing BC-provider");
        CryptoProviderTools.removeBCProvider();
        CryptoProviderTools.installBCProvider();

        // Run java seed collector, that can take a little time the first time it is run
        log.trace(">init initializing random seed");
        SecureRandom rand = new SecureRandom();
        rand.nextInt();
        
        //
        // Start services that requires calling other beans or components
        //
        
        // We really need BC to be installed. This is an attempt to fix a bug where the ServiceSessionBean
        // crashes from not finding the BC-provider.
        int waitTime = 0;
        while (Security.getProvider("BC") == null && waitTime++ < 5) {
        	log.info("Waiting for BC provider to be installed..");
        	try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log("Waiting for BC provider failed.", e);
				break;
			}
        }

        // We have to read CAs into cache (and upgrade them) early, because the log system may use CAs for signing logs
        
		log.trace(">init CryptoTokenFactory just to load those classes that are available");
		CryptoTokenFactory.instance();
		
        // Load CAs at startup to improve impression of speed the first time a CA is accessed, it takes a little time to load it.
        log.trace(">init loading CAs into cache");
        try {
        	caAdminSession.initializeAndUpgradeCAs();
        } catch (Exception e) {
        	log.error("Error creating CAAdminSession: ", e);
        }

    	AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("StartServicesServlet.init"));

        // Make a log row that EJBCA is starting
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("msg", iMsg);
        logSession.log(EjbcaEventTypes.EJBCA_STARTING, EventStatus.SUCCESS, EjbcaModuleTypes.SERVICE, EjbcaServiceTypes.EJBCA, admin.toString(), null, getHostName(), null, details);				

        // Log the type of security audit configuration that we have enabled.
        log.trace(">init security audit device configuration");
        final Set<String> loggerIds = AuditDevicesConfig.getAllDeviceIds();
        if (loggerIds.isEmpty()) {
            final String msg = intres.getLocalizedMessage("startservices.noauditdevices");
            log.info(msg);
        } else {
            if (!checkForProtectedAudit(admin, loggerIds)) {
                // Make a log row that no integrity protected device is configured
                final String msg = intres.getLocalizedMessage("startservices.noprotectedauditdevices");
                final Map<String, Object> logdetails = new LinkedHashMap<String, Object>();
                logdetails.put("msg", msg);
                logSession.log(EventTypes.LOG_MANAGEMENT_CHANGE, EventStatus.VOID, ModuleTypes.SECURITY_AUDIT, ServiceTypes.CORE, admin.toString(), null, null, null, logdetails);                
            }
        }

        // Initialize authorization system, if not done already
        log.trace(">init ComplexAccessControlSession to check for initial root role");
        complexAccessControlSession.initializeAuthorizationModule();

        log.trace(">init calling ServiceSession.load");
        try {
        	serviceSession.load();
		} catch (Exception e) {
			log.error("Error init ServiceSession: ", e);
		}
		
        // Load Certificate profiles at startup to upgrade them if needed
        log.trace(">init loading CertificateProfile to check for upgrades");
        try {
        	certificateProfileSession.initializeAndUpgradeProfiles();
        } catch (Exception e) {
        	log.error("Error initializing certificate profiles: ", e);
        }
        
        // Load EndEntity profiles at startup to upgrade them if needed
        // And add this node to list of nodes
        log.trace(">init loading EndEntityProfile to check for upgrades");
        try {
        	endEntityProfileSession.initializeAndUpgradeProfiles();        	
        } catch (Exception e) {
        	log.error("Error initializing end entity profiles: ", e);
        }
        
        // Add this node's hostname to list of nodes
        log.trace(">init checking if this node is in the list of nodes");
        try {
            // Requires a transaction in order to create the initial global configuration
            tx.begin();
            try {
                final GlobalConfiguration config = (GlobalConfiguration) globalConfigurationSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
                final Set<String> nodes = config.getNodesInCluster();
                final String hostname = getHostName();
                if (hostname != null && !nodes.contains(hostname)) {
                    log.debug("Adding this node ("+hostname+") to the list of nodes");
                    nodes.add(hostname);
                    config.setNodesInCluster(nodes);
                    globalConfigurationSession.saveConfiguration(admin, config);
                }
            } finally {
                tx.commit();
            }
        } catch (Exception e) {
            log.error("Error adding host to node list in global configuration: ", e);
        } 

        log.trace(">init SignSession to check for unique issuerDN,serialNumber index");
        // Call the check for unique index, since first invocation will perform the database
        // operation and avoid a performance hit for the first request where this is checked.
        certCreateSession.isUniqueCertificateSerialNumberIndex();       
        
        /*
         * FIXME: This is a hack, because we need some sort of annotation or service loader to make sure 
         * that the AccessMatchValue-implementing enums get initialized at runtime. Sadly, enums aren't 
         * initialized until they're called, which causes trouble with this registry. 
         * 
         * These lines are to be removed once a dynamic initialization heuristic has been developed.
         * 
         */      
        try {
            Class.forName(X500PrincipalAccessMatchValue.class.getName());
            Class.forName(CliUserAccessMatchValue.class.getName());
        } catch (ClassNotFoundException e) {
            log.error("Failure during match value initialization", e);
        }
        // Check if there the default responder has been set. If not, try setting it using the old value.
        GlobalOcspConfiguration globalConfiguration = (GlobalOcspConfiguration) globalConfigurationSession
                .getCachedConfiguration(GlobalOcspConfiguration.OCSP_CONFIGURATION_ID);
        if (StringUtils.isEmpty(globalConfiguration.getOcspDefaultResponderReference())) {
            globalConfiguration.setOcspDefaultResponderReference(OcspConfiguration.getDefaultResponderId());
            try {
                globalConfigurationSession.saveConfiguration(admin, globalConfiguration);
                globalConfigurationSession.flushConfigurationCache(GlobalOcspConfiguration.OCSP_CONFIGURATION_ID);
            } catch (AuthorizationDeniedException e) {
                throw new IllegalStateException(
                        "An always allow token was not allowed access. Likely cause is that the database hasn't been configured.");
            }
        }
        // Upgrade the old Validation Authority Publisher in Community Edition (leave it be in Enterprise for the sake of 100% uptime)
        if(!enterpriseEditionEjbBridgeSession.isRunningEnterprise()) {
            publisherSession.adhocUpgradeTo6_3_1_1();
        }
        
        // Check and upgrade if this is the first time we start an instance that was previously an stand-alone VA
        ocspResponseGeneratorSession.adhocUpgradeFromPre60(null);
        // Start key reload timer
        ocspResponseGeneratorSession.initTimers();
        // Start CA certificate cache reload
        certificateStoreSession.initTimers();
        // Verify that the EJB CLI user (if present) cannot be used to generate certificates
        final String cliUsername = EjbcaConfiguration.getCliDefaultUser();
        try {
            final EndEntityInformation defaultCliUser = endEntityAccessSession.findUser(admin, cliUsername);
            if (defaultCliUser!=null && defaultCliUser.getStatus()==EndEntityConstants.STATUS_NEW) {
                try {
                    endEntityManagementSession.setUserStatus(admin, cliUsername, EndEntityConstants.STATUS_GENERATED);
                } catch (ApprovalException e) {
                    log.warn("The EJBCA CLI user '" + cliUsername + "' could be used for certificate enrollment. Please correct the status manually. Failed with: " + e.getMessage());
                } catch (FinderException e) {
                    log.warn("The EJBCA CLI user '" + cliUsername + "' could be used for certificate enrollment. Please correct the status manually. Failed with: " + e.getMessage());
                } catch (WaitingForApprovalException e) {
                    log.warn("The EJBCA CLI user '" + cliUsername + "' could be used for certificate enrollment. Please correct the status manually. Failed with: " + e.getMessage());
                }
            }
        } catch (AuthorizationDeniedException e) {
            log.warn("Unable to check if the EJBCA CLI user '" + cliUsername + "' could be used for certificate enrollment. Please check and correct the status manually. Failed with: " + e.getMessage());
        }
    }

    /** Method that checks if we have an integrity protected security audit device configured, and in that case logs the configuration startup 
     * 
     * @param admin an authentication token used to log the configuration management startup (logged as a change as audit is configured during startup from properties file) 
     * @param loggerIds the configured loggers among which we look for the protected device
     * @return true if there is an integrity protected audit device and is was configured during startup (and audit log of this config was made)
     */
    private boolean checkForProtectedAudit(AuthenticationToken admin, final Set<String> loggerIds) {
        boolean ret = false;                                            
        // See if we have IntegrityProtectedDevice configured, due to class loading constraints we can not use IntegrityProtectedDevice.class.getSimpleName().
        // This is admin-gui and does not have access to that class
        final String integrityProtectedName = "IntegrityProtectedDevice";
        for (Iterator<String> iterator = loggerIds.iterator(); iterator.hasNext();) {
            final String id = (String) iterator.next();
            if (integrityProtectedName.equals(id)) {
                // Make a log row that integrity protected device is configured
                final Map<String, Object> logdetails = new LinkedHashMap<String, Object>();
                try {
                    // Use reflection to get the ProtectedDataConfiguration and make some calls to it.
                    // This is needed since ProtectedDataConfiguration may not be available during compile time, or runtime
                    Class<?> c = Class.forName("org.cesecore.dbprotection.ProtectedDataConfiguration");
                    // create instance ProtectedDataConfiguration.instance()
                    Method instance = c.getMethod("instance", (Class[])null);
                    //Object[] args = new Object[0];
                    Object config = instance.invoke(null);
                    // create method ProtectedDataConfiguration.getKeyId(String)
                    Method getKeyId = c.getMethod("getKeyId", String.class);
                    // create method ProtectedDataConfiguration.getProtectVersion(int)
                    Method getProtectVersion = c.getMethod("getProtectVersion", Integer.TYPE);
                    // create method ProtectedDataConfiguration.getKeyLabel(int)
                    Method getKeyLabel = c.getMethod("getKeyLabel", Integer.TYPE);
                    // Call ProtectedDataConfiguration.instance().getKeyId
                    final String auditTableName = AuditRecordData.class.getSimpleName();
                    final Integer keyid = (Integer)getKeyId.invoke(config, auditTableName);
                    if ((keyid != null) && (keyid > 0)) {
                        if (CesecoreConfiguration.useDatabaseIntegrityProtection(auditTableName)) {
                            // Call ProtectedDataConfiguration.instance().getProtectVersion
                            final Integer protectVersion = (Integer)getProtectVersion.invoke(config, keyid);
                            // Call ProtectedDataConfiguration.instance().getKeyLabel
                            final String keyLabel = (String)getKeyLabel.invoke(config, keyid);
                            logdetails.put("keyid", keyid);
                            logdetails.put("protectVersion", protectVersion);
                            logdetails.put("keyLabel", keyLabel);
                            logSession.log(EventTypes.LOG_MANAGEMENT_CHANGE, EventStatus.SUCCESS, ModuleTypes.SECURITY_AUDIT, ServiceTypes.CORE, admin.toString(), null, null, null, logdetails);
                            ret = true;                                            
                        } else {
                            log.debug("No database integrity protection enabled for AuditRecordData.");
                        }
                    } else {
                        log.debug("No keyid configured for AuditRecordData.");
                    }
                } catch (ClassNotFoundException e) {
                    log.info("No database integrity protection available in this version of EJBCA.");
                } catch (IllegalAccessException e) {
                    log.info("No database integrity protection available due to initialization error: ", e);
                } catch (SecurityException e) {
                    log.info("No database integrity protection available due to initialization error: ", e);
                } catch (NoSuchMethodException e) {
                    log.info("No database integrity protection available due to initialization error: ", e);
                } catch (IllegalArgumentException e) {
                    log.info("No database integrity protection available due to initialization error: ", e);
                } catch (InvocationTargetException e) {
                    log.info("No database integrity protection available due to initialization error: ", e);
                }
            }
        }
        return ret;
    }
    
    /**
     * @return The host's name or null if it could not be determined.
     */
    private String getHostName() {
    	String hostname = null;
    	try {
	        InetAddress addr = InetAddress.getLocalHost();    
	        // Get hostname
	        hostname = addr.getHostName();
	    } catch (UnknownHostException e) {
	    	log.error("Hostname could not be determined", e);
	    }
	    return hostname;
    }
    
}
