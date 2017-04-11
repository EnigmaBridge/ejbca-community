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
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.StringTools;
import org.cesecore.vpn.VpnUser;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.ejb.vpn.useragent.OperatingSystem;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.pub.ServletUtils;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
    private static final String OTP_COOKIE = "ebvpn_otp_cookie";

    @EJB
    private VpnUserManagementSessionLocal vpnUserManagementSession;

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
            final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(new UsernamePrincipal("VpnDownloadServlet: "+request.getRemoteAddr()));
            final GlobalConfiguration globalConfiguration = (GlobalConfiguration) globalConfigurationSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
            RequestHelper.setDefaultCharacterEncoding(request);
            ServletUtils.removeCacheHeaders(response);

            final String otp = request.getParameter("otp");
            final String vpnUserIdTxt = request.getParameter("id");
            final int vpnUserId = Integer.parseInt(vpnUserIdTxt);
            final String cookieName = OTP_COOKIE;

            final List<Cookie> matchingCookies = VpnWebUtils.getCookies(request, cookieName);
            final Cookie cookie = !matchingCookies.isEmpty() ? matchingCookies.get(0) : null;

            final Properties properties = new Properties();
            VpnBean.buildDescriptorProperties(request, properties);

            final String xFwded = request.getHeader("X-Forwarded-For");
            final String ip = request.getRemoteAddr();
            final String sourceAddr = ip + ";" + xFwded;
            final String ua = request.getHeader("User-Agent");
            final String method = request.getMethod();
            final String cookieValue = cookie == null ? null : cookie.getValue();

            VpnLinkError vpnError = VpnLinkError.NONE;
            VpnUser vpnUser = null;
            try {
                vpnUser = vpnUserManagementSession.downloadOtp(admin, vpnUserId, otp, cookieValue, properties);

                final Cookie newCookie = new Cookie(cookieName, vpnUser.getOtpCookie());
                newCookie.setMaxAge(600);   // 10 minutes validity
                newCookie.setSecure(true);  // cookie should be sent only over a secure channel
                response.addCookie(newCookie);

                final Cookie downloadCookie = new Cookie(VpnBean.DOWNLOADED_COOKIE, "true");
                downloadCookie.setMaxAge(600);   // 10 minutes validity
                downloadCookie.setSecure(true);  // cookie should be sent only over a secure channel
                downloadCookie.setPath("/");     // universal path - reachable in the config.jsf
                response.addCookie(downloadCookie);

            } catch (VpnOtpOldException e) {
                vpnError = VpnLinkError.OTP_OLD;
                log.info(String.format("OTP failed - too old. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpTooManyException e) {
                vpnError = VpnLinkError.OTP_TOO_MANY;
                log.info(String.format("OTP failed - too many. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpCookieException e) {
                vpnError = VpnLinkError.OTP_COOKIE;
                log.info(String.format("OTP failed - cookie. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpDescriptorException e) {
                vpnError = VpnLinkError.OTP_DESCRIPTOR;
                log.info(String.format("OTP failed - descriptor. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpInvalidException e) {
                vpnError = VpnLinkError.OTP_INVALID;
                log.info(String.format("OTP failed - invalid. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnNoConfigException e) {
                vpnError = VpnLinkError.NO_CONFIGURATION;
                log.info(String.format("OTP failed - config empty. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));

            } catch (Exception e){
                vpnError = VpnLinkError.GENERIC;
                log.info(String.format("OTP failed - generic. ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue), e);
            }

            if (vpnUser == null){
                // Set error to the session - picked up by the VpnBean.
                request.getSession().setAttribute(VpnBean.LINK_ERROR_SESSION, vpnError.toString());
                response.sendRedirect("key.jsf");

            } else {
                final VpnGenOptions genOptions = new VpnGenOptions();
                final String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) {
                    genOptions.setOs(OperatingSystem.parseUserAgentString(userAgent));
                }

                final String fileName = VpnUtils.genVpnConfigFileNameHuman(vpnUser, genOptions);
                response.setContentType("application/ovpn");
                response.setHeader("Content-disposition", " attachment; filename=\"" + StringTools.stripFilename(fileName) + "\"");

                // Cookie this OTP was downloaded already, survives browser restart
                final Cookie downloadCookie = new Cookie(VpnBean.LAST_OTP_TOKEN_DOWNLOADED, otp);
                downloadCookie.setMaxAge(3600);   // 60 minutes validity
                downloadCookie.setSecure(true);  // cookie should be sent only over a secure channel
                downloadCookie.setPath("/");     // universal path - reachable in the config.jsf
                response.addCookie(downloadCookie);

                final String vpnConfig = vpnUserManagementSession.generateVpnConfig(admin, vpnUser, genOptions);
                final byte[] bytes2send = vpnConfig.getBytes("UTF-8");
                response.setContentLength(bytes2send.length);
                response.getOutputStream().write(bytes2send);

                request.getSession().setAttribute(VpnBean.LAST_OTP_TOKEN_DOWNLOADED, otp);

                log.info(String.format("OTP download OK ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        vpnUserId, otp, sourceAddr, ua, method, cookieValue));
            }

            response.flushBuffer();

        } catch (Exception e) {
            throw new ServletException(e);
        }
        log.trace("<doGet()");
    }   
}
