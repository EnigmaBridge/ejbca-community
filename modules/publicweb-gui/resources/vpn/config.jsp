<%@ page import="org.ejbca.core.ejb.vpn.*" %>
<%@ page import="org.ejbca.ui.web.pub.vpn.VpnLinkError" %>
<%@ page import="org.ejbca.core.ejb.vpn.useragent.OperatingSystem" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%
    response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding());
    org.ejbca.ui.web.RequestHelper.setDefaultCharacterEncoding(request);

%>

<jsp:useBean id="vpnBean" class="org.ejbca.ui.web.pub.vpn.VpnBean" scope="session" />
<%
    vpnBean.initialize(request);
%>
<f:view>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=<%= org.ejbca.config.WebConfiguration.getWebContentEncoding() %>" />
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=0">
    <title><%= org.ejbca.config.InternalConfiguration.getAppNameCapital() %> Public Web</title>
    <link rel="shortcut icon" href="../images/favicon.png" type="image/png" />
    <link rel="stylesheet" href="../scripts/bootstrap-3.3.7.min.css" type="text/css" />
    <link rel="stylesheet" href="../scripts/vpnstyle.css" type="text/css" />
    <script type="text/javascript" src="../scripts/jquery-1.12.4.min.js"></script>
    <script type="text/javascript" src="../scripts/bootstrap-3.3.7.min.js"></script>
</head>
<body>

<div class="jumbotron text-center">
    <h1>VPN Configuration</h1>
    <p>VPN client configuration download</p>
</div>

<div class="container">
    <h:outputText value="" rendered="#{vpnBean.otpValid}"/>

    <c:choose>
        <c:when test="${vpnBean.otpValid}">
            <div id="vpnInfo" class="form-group" style="display: block;">
                <div class="panel panel-default">
                    <div class="panel-heading">VPN configuration details</div>
                    <div class="panel-body" id="pre-info">
                        <table class="table">
                            <tr>
                                <th style="border-top: none">Email</th>
                                <td style="border-top: none">${vpnBean.vpnUser.email}</td>
                            </tr>
                            <tr>
                                <th>Device</th>
                                <td>${vpnBean.vpnUser.device}</td>
                            </tr>
                            <tr>
                                <th>Generated</th>
                                <td>
                                    <h:outputText id="dateGenerated" value="#{vpnBean.dateGenerated}">
                                        <f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
                                    </h:outputText>
                                </td>
                            </tr>
                        </table>
                </div>
            </div>

            <div id="divButtonUpload">
                <a class="btn btn-primary btn-rich-electric-blue btn-xl btn-block btn-wrap" id="btnDownload"
                   href="getvpn?id=${vpnBean.vpnUserId}&otp=${vpnBean.otp}">Download</a>
            </div>

            <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID
                    || vpnBean.getOsGroup() == OperatingSystem.IOS
                    || vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Suitable VpnClient</h3>
                        <p>Downloaded VPN configuration file needs to be opened in the appropriate VPN client.</p>
                        <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID) { %>
                        <p>
                            <a href="https://play.google.com/store/apps/details?id=net.openvpn.openvpn"
                               rel="nofollow" target="_blank">OpenVPN Connect</a> for Android.
                        </p>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.IOS) { %>
                        <p>
                            <a href="https://itunes.apple.com/us/app/openvpn-connect/id590379981?mt=8"
                               rel="nofollow" target="_blank">OpenVPN Connect</a> for iOS.
                        </p>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS) { %>
                        <p>
                            <a href="https://openvpn.net"
                               rel="nofollow" target="_blank">OpenVPN</a> client for Windows.
                        </p>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.LINUX) { %>
                        <p>
                            <a href="https://openvpn.net"
                               rel="nofollow" target="_blank">OpenVPN</a> client for Linux.
                        </p>

                        <h3>Installation using package managers</h3>
                        <div class="panel panel-default">
                            <div class="panel-heading">Yum</div>
                            <div class="panel-body pre-block">sudo yum install openvpn</div>
                        </div>

                        <div class="panel panel-default">
                            <div class="panel-heading">apt-get</div>
                            <div class="panel-body pre-block">sudo apt-get install openvpn</div>
                        </div>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>
                        <p>
                            <a href="https://tunnelblick.net/"
                               rel="nofollow" target="_blank">Tunnelblick</a> for MAC OS.
                        </p>

                        <% } %>
                    </div>
                </div>
            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Troubleshooting</h3>
                    <p>
                        In case of a problem please consult your administrator.
                    </p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div id="divError" class="alert alert-danger fade in">
                <strong>Error:</strong>
                <% if (vpnBean.getLinkError() == VpnLinkError.OTP_INVALID) { %>
                Link is invalid
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_OLD) { %>
                Link is too old
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_TOO_MANY) { %>
                Link used too many times
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_COOKIE) { %>
                Link cookie exception
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_DESCRIPTOR) { %>
                Link invalid device
                <% } else if (vpnBean.getLinkError() == VpnLinkError.NO_CONFIGURATION) { %>
                Configuration have already been downloaded
                <% } else if (vpnBean.getLinkError() == VpnLinkError.GENERIC) { %>
                Link is invalid - generic error
                <% } else { %>
                No link
                <% } %>
            </div>

            <div class="row">
                <div class="col-sm-12">
                    <h3>This link is invalid</h3>
                    <p>VPN Configuration download link is invalid and cannot be used.
                        It may be expired or already downloaded. </p>
                    <p>Please contact your administrator to issue a new VPN configuration key.</p>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

</body>
</html>
</f:view>

