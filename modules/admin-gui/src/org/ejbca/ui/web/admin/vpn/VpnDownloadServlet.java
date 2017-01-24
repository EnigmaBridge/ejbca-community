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
package org.ejbca.ui.web.admin.vpn;

import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AlwaysAllowLocalAuthenticationToken;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.UsernamePrincipal;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.ejb.vpn.VpnUserManagementSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for download of VPN configuration files and VPN related files.
 *
 * @author ph4r05
 */
public class VpnDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(VpnDownloadServlet.class);
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();
    private static final AuthenticationToken alwaysAllowAuthenticationToken = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("VpnDownloadServlet"));

    @EJB
    private VpnUserManagementSessionLocal vpnUserManagementSession;

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
            response.setStatus(500);
            response.flushBuffer();

        } catch (Exception e) {
            throw new ServletException(e);
        }
        log.trace("<doGet()");
    }   
}
