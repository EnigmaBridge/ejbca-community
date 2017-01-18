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
package org.ejbca.ui.web.pub.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.StringTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.ejb.vpn.VpnConfig;
import org.ejbca.core.ejb.vpn.VpnUserManagementSession;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.vpn.VpnUtils;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.ui.web.RequestHelper;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

/**
 * Servlet for download of VPN configuration files and VPN related files.
 *
 * @author ph4r05
 */
public class VpnDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(VpnDownloadServlet.class);
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    @EJB
    private VpnUserManagementSession vpnUserManagementSession;
    @EJB
    private GlobalConfigurationSessionLocal globalConfigurationSession;

    // org.cesecore.keys.util.KeyTools.getAsPem(PublicKey)
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        CryptoProviderTools.installBCProviderIfNotAvailable();
    }

    /** Handles HTTP POST the same way HTTP GET is handled. */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    /** Handles HTTP GET */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.trace(">doGet()");
        try {
            AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("VpnDownloadServlet: "+request.getRemoteAddr()));
            GlobalConfiguration globalConfiguration = (GlobalConfiguration) globalConfigurationSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
            RequestHelper.setDefaultCharacterEncoding(request);

            final String otp = request.getParameter("otp");
            final String vpnUserIdTxt = request.getParameter("id");
            final int vpnUserId = Integer.parseInt(vpnUserIdTxt);

            final Properties properties = new Properties();
            final String xFwded = request.getHeader("X-Forwarded-For");
            final String ip = request.getRemoteAddr();
            final String sourceAddr = ip + ";" + xFwded;
            final String ua = request.getHeader("User-Agent");
            properties.setProperty("ip", ip+"");
            properties.setProperty("fwded", xFwded+"");
            properties.setProperty("ua", ua+"");

            final VpnUser vpnUser = vpnUserManagementSession.downloadOtp(admin, vpnUserId, otp, properties);
            if (vpnUser == null){
                // TODO: redirect to some nice looking page explaining what happened.
                log.info(String.format("OTP auth failed with ID: %d, OTP[%s], src: %s, ua: %s", vpnUserId, otp, sourceAddr, ua));
                response.setStatus(404);

            } else {
                final String fileName = VpnUtils.genVpnConfigFileName(vpnUser);
                response.setContentType("application/ovpn");
                response.setHeader("Content-disposition", " attachment; filename=\"" + StringTools.stripFilename(fileName) + "\"");
                response.getOutputStream().write(vpnUser.getVpnConfig().getBytes("UTF-8"));
            }

            response.flushBuffer();

        } catch (AuthorizationDeniedException e) {
            throw new ServletException(e);
        }
        log.trace("<doGet()");
    }   
}
