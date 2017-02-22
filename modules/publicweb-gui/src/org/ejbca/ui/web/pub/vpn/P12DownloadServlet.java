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
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.vpn.*;
import org.ejbca.core.model.InternalEjbcaResources;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.pub.ServletUtils;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Servlet for download of VPN configuration files and VPN related files.
 *
 * @author ph4r05
 */
public class P12DownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(P12DownloadServlet.class);
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();
    private static final String OTP_COOKIE = "ebvpn_p12_otp_cookie";

    @EJB
    private VpnUserManagementSessionLocal vpnUserManagementSession;

    @EJB
    private GlobalConfigurationSessionLocal globalConfigurationSession;

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
            final AuthenticationToken admin = new AlwaysAllowLocalAuthenticationToken(
                    new UsernamePrincipal("P12DownloadServlet: "+request.getRemoteAddr()));

            final GlobalConfiguration globalConfiguration = (GlobalConfiguration) globalConfigurationSession
                    .getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);

            RequestHelper.setDefaultCharacterEncoding(request);
            ServletUtils.removeCacheHeaders(response);

            final String otp = request.getParameter("otp");
            final String cookie_name = OTP_COOKIE;

            Cookie cookie = null;
            final Cookie[] cookies = request.getCookies();
            if (cookies != null){
                for (Cookie curCookie : cookies) {
                    if (cookie_name.equals(curCookie.getName())){
                        cookie = curCookie;
                        break;
                    }
                }
            }

            final Properties properties = new Properties();
            VpnBean.buildDescriptorProperties(request, properties);

            final String xFwded = request.getHeader("X-Forwarded-For");
            final String ip = request.getRemoteAddr();
            final String sourceAddr = ip + ";" + xFwded;
            final String ua = request.getHeader("User-Agent");
            final String method = request.getMethod();
            final String cookieValue = cookie == null ? null : cookie.getValue();

            VpnLinkError linkError = VpnLinkError.NONE;
            OtpDownload token = null;

            try {
                if (!VpnUtils.isIpInVPNNetwork(ip)){
                    throw new VpnOtpNoVpnException();
                }

                token = vpnUserManagementSession.otpDownloadOtp(admin, otp, cookieValue, properties);

                final Cookie newCookie = new Cookie(cookie_name, token.getOtpCookie());
                newCookie.setMaxAge(600);   // 10 minutes validity
                newCookie.setSecure(true);  // cookie should be sent only over a secure channel
                response.addCookie(newCookie);

                final Cookie downloadCookie = new Cookie(P12Bean.DOWNLOADED_COOKIE, "true");
                downloadCookie.setMaxAge(600);   // 10 minutes validity
                downloadCookie.setSecure(true);  // cookie should be sent only over a secure channel
                downloadCookie.setPath("/");     // universal path - reachable in the config.jsf
                response.addCookie(downloadCookie);

            } catch (VpnOtpOldException e) {
                linkError = VpnLinkError.OTP_OLD;
                log.info(String.format("OTP p12 failed - too old. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpTooManyException e) {
                linkError = VpnLinkError.OTP_TOO_MANY;
                log.info(String.format("OTP p12 failed - too many. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpCookieException e) {
                linkError = VpnLinkError.OTP_COOKIE;
                log.info(String.format("OTP p12 failed - cookie. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpDescriptorException e) {
                linkError = VpnLinkError.OTP_DESCRIPTOR;
                log.info(String.format("OTP p12 failed - descriptor. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpInvalidException e) {
                linkError = VpnLinkError.OTP_INVALID;
                log.info(String.format("OTP p12 failed - invalid. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (VpnOtpNoVpnException e) {
                linkError = VpnLinkError.NOT_IN_VPN;
                log.info(String.format("OTP p12 failed - no VPN. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue));

            } catch (Exception e){
                linkError = VpnLinkError.GENERIC;
                log.info(String.format("OTP p12 failed - generic. OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        otp, sourceAddr, ua, method, cookieValue), e);
            }

            if (token == null){
                // Set error to the session - picked up by the VpnBean.
                request.getSession().setAttribute(P12Bean.LINK_ERROR_SESSION, linkError.toString());
                response.sendRedirect("p12.jsf");

            } else {

                final String fileName = VpnUtils.getP12FileNameHuman(token);
                response.setContentType("application/x-pkcs12");
                response.setHeader("Content-disposition", " attachment; filename=\"" + StringTools.stripFilename(fileName) + "\"");

                final String auxJson = token.getAuxData();
                final JSONObject aux = new JSONObject(auxJson);
                final String p12Path = aux.getString(VpnCons.OTP_AUX_P12_PATH);
                final File p12File = new File(p12Path);

                byte[] bytes2send = new byte[(int) p12File.length()];
                final FileInputStream fis = new FileInputStream(p12File);
                fis.read(bytes2send);
                fis.close();

                response.setContentLength(bytes2send.length);
                response.getOutputStream().write(bytes2send);

                log.info(String.format("OTP p12 download OK ID: %d, OTP[%s], src: %s, ua: %s, method: %s, cookie: %s",
                        token.getId(), otp, sourceAddr, ua, method, cookieValue));
            }

            response.flushBuffer();

        } catch (Exception e) {
            throw new ServletException(e);
        }
        log.trace("<doGet()");
    }   
}
