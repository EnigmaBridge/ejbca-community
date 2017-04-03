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

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authentication.tokens.X509CertificateAuthenticationToken;
import org.cesecore.authorization.control.StandardRules;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.ejbca.config.WebConfiguration;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.model.util.EjbLocalHelper;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter injecting HTTP user to the chain for the auth - vpn auth.
 * Watching for local port change.
 * 
 * @version $Id: VpnSessionFilter.java 19902 2014-09-30 14:32:24Z ph4r05 $
 */
public class VpnSessionFilter implements Filter {
	private static final Logger log = Logger.getLogger(VpnSessionFilter.class);

	private static final String ATTR_X509CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String ATTR_EJBCA_LOCAL_PORT = "ejbcaLocalPort";
    private static final String ATTR_EJBCA_REMOTE_ADDR = "ejbcaClientRemoteAddr";
    private static final String ATTR_EJBCA_CLIENT_CERT = "ejbcaVpnAuthClientCert";
    public static final String ATTR_EJBCA_IDENTITY_VERIFIED = "ejbcaIdentityVerified";

	private boolean proxiedAuthenticationEnabled = false;
	private boolean cacheCertToSession = true;
	
	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	    proxiedAuthenticationEnabled = WebConfiguration.isProxiedAuthenticationEnabled();
	}

	@Override
	public void destroy() {
	}

	/**
	 * If there is some port defined and is different from current, destroy session and reload.
	 * Port change keeps the same session but breaks the internals... Workaround is to invalidate the session.
	 *
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return boolean true if session is OK
	 */
	private boolean handleSession(HttpServletRequest request, HttpServletResponse response){
		final HttpSession session = request.getSession();
		if (session == null){
			log.debug("No session yet");
			return true;
		}

		try{
			final int curLocalPort = request.getLocalPort();
			final Object localPortObj = session.getAttribute(ATTR_EJBCA_LOCAL_PORT);
			if (localPortObj == null){
				session.setAttribute(ATTR_EJBCA_LOCAL_PORT, curLocalPort);
				return true;
			}

			final Integer localPort = (Integer) localPortObj;
			if (localPort == curLocalPort){
				return true;
			}

			log.info(String.format("Port change detected, previous: %s, current: %s, session: %s",
					localPort, curLocalPort, session));
			session.invalidate();

			// Reload the page
			response.sendRedirect(request.getContextPath() + request.getServletPath());
			return false;

		} catch (Exception e){
			log.error("Exception in session handling", e);
			return false;
		}
	}

	/**
	 * Returns true if the current request conveys client certificate
	 * @param request HttpServletRequest
	 * @return true if certificate is present
	 */
	private boolean hasClientCert(HttpServletRequest request){
		try{
			return request.getAttribute(ATTR_X509CERTIFICATE) != null;
		} catch(Throwable ex){
			return false;
		}
	}

	/**
	 * Returns true if the cached cert is OK
	 * @return true if cached certificate seems OK
	 */
	private boolean checkCachedCert(Object certs){
		if (certs == null){
			return false;
		}

		if (!(certs instanceof X509Certificate[])){
			return false;
		}

		X509Certificate[] c = (X509Certificate[]) certs;
		if (c.length == 0){
			return false;
		}

		if (c[0] == null){
			return false;
		}

		return true;
	}

	/**
	 * Flushes cached certificate on IP change forcing to do vpn auth again.
	 * @param session session associated to the request
	 */
	private void flushCacheOnIpChange(HttpServletRequest request, HttpSession session){
		final Object remoteAddrSession = session.getAttribute(ATTR_EJBCA_REMOTE_ADDR);
		final String remoteAddrRequest = request.getRemoteAddr();

		if (remoteAddrSession == null){
			session.setAttribute(ATTR_EJBCA_REMOTE_ADDR, remoteAddrRequest);
			return;
		}

		if (remoteAddrRequest == null){
			log.info("Remote address is null, should not happen");
			return;
		}

		if (!remoteAddrRequest.equals(remoteAddrSession)){
			log.info(String.format("Remote address change from %s to %s", remoteAddrSession, remoteAddrRequest));
			session.setAttribute(ATTR_EJBCA_CLIENT_CERT, null);
			session.setAttribute(ATTR_EJBCA_REMOTE_ADDR, remoteAddrRequest);
		}
	}

	@Override
	public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain filterChain) throws IOException, ServletException {
		final StopWatch sw = new StopWatch();
		sw.start();

		try {
			final HttpServletRequest request = (HttpServletRequest) req;
			final HttpServletResponse response = (HttpServletResponse) res;

			// If there is some port defined and is different from current, destroy session and reload.
			if (!handleSession(request, response)){
				return;
			}

			// If client cert is present, nothing to do.
			if (hasClientCert(request)){
				filterChain.doFilter(req, res);
				return;
			}

			// Cached certificate in the session
			final HttpSession session = request.getSession();
			flushCacheOnIpChange(request, session);

			final Object cachedCerts = session.getAttribute(ATTR_EJBCA_CLIENT_CERT);
			if (cacheCertToSession && checkCachedCert(cachedCerts)){
				log.info("Authenticating with Cached VpnAuth, time: " + sw.getTime() + " ms");
				request.setAttribute(ATTR_X509CERTIFICATE, cachedCerts);
				filterChain.doFilter(req, res);
				return;
			}

			// Non-cached version
			final EjbLocalHelper ejb = new EjbLocalHelper();
			final org.ejbca.core.ejb.vpn.VpnWebUtils.AdminAuthorization adminAuth = new org.ejbca.core.ejb.vpn.VpnWebUtils.AdminAuthorization(
					ejb.getWebAuthenticationProviderSession(),
					ejb.getEndEntityManagementSession(),
					ejb.getAccessControlSession(),
					ejb.getVpnUserManagementSession(),
					ejb.getCertificateStoreSession());

			if (adminAuth.tryIsAuthorizedVpn(request)){
				log.info("Authenticating with VpnAuth");
				X509Certificate[] certificates = adminAuth.getCertificates();
				session.setAttribute(ATTR_EJBCA_CLIENT_CERT, cacheCertToSession ? certificates : null);
				request.setAttribute(ATTR_X509CERTIFICATE, certificates);

			} else{
				session.setAttribute(ATTR_EJBCA_CLIENT_CERT, null);
				request.setAttribute(ATTR_X509CERTIFICATE, null);
			}

			log.info("Total VpnAuth overhead: " + sw.getTime() + " ms");

		} catch (Exception ex) {
			log.error("Exception in filtering", ex);
		}

    	filterChain.doFilter(req, res);
	}
	
	@SuppressWarnings("Duplicates")
	private void showError(final HttpServletResponse httpServletResponse, final String content) throws IOException {
        httpServletResponse.setContentType("text/html; charset=UTF-8");
        httpServletResponse.setHeader("pragma", "no-cache");
        httpServletResponse.setHeader("cache-control", "no-cache");
        httpServletResponse.setHeader("expires", "-1");
        httpServletResponse.setContentLength(content.length());
        final OutputStream os = httpServletResponse.getOutputStream();
        os.write(content.getBytes());
        os.flush();
        os.close();
	}
}
