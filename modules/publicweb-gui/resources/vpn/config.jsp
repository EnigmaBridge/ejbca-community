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
    <script type="text/javascript" src="../scripts/jquery.qrcode.min.js"></script>
    <script type="text/javascript" src="../scripts/bootstrap-3.3.7.min.js"></script>
    <script type="text/javascript" src="../scripts/vpnfunctions.js"></script>
    <script type="text/javascript">
        $(document).on("click", "#btnDownload", function () {
            $.fileDownload($(this).prop('href'), {
                'dialogOptions': {
                    'modal': false
                }})
                .done(function () {
                    $( "#divStatusNotif" ).show( "slow" );
                    $( "#divButtonUpload" ).hide( "slow" );
                })
                .fail(function () {
                });

            return false; //this is critical to stop the click event which will trigger a normal file download
        });

        function regenerateQrCode(link){
            var divQrCode = $('#qrcode');
            divQrCode.html("");

            if (!link || 0 === link.length) {
                return;
            }

            var qrCodeSettings = {
                "render": "canvas",
                "text": link,
                "size": 300
            };
            divQrCode.qrcode(qrCodeSettings);
        }

        $(function() {
            regenerateQrCode('${!vpnBean.landingLink != null ? vpnBean.landingLink : ""}');
        });
    </script>
</head>
<body>

<div class="jumbotron text-center">
    <h1>Your Private Space</h1>
    <p>Enigma Bridge Private Space key download</p>
</div>

<div class="container">
    <h:outputText value="" rendered="#{vpnBean.otpValid}"/>

    <c:choose>
        <c:when test="${vpnBean.otpValid}">
            <div id="vpnInfo" class="form-group" style="display: block;">
                <div class="panel panel-default">
                    <div class="panel-heading">Private space user key</div>
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

            <div id="divStatusNotif" class="alert alert-success" style="display: none;">Download successful</div>

            <div id="divButtonUpload">
                <a class="btn btn-primary btn-rich-electric-blue btn-xl btn-block btn-wrap" id="btnDownload"
                   href="${vpnBean.downloadLink}">Download</a>
            </div>

            <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID
                    || vpnBean.getOsGroup() == OperatingSystem.IOS
                    || vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Suitable VPN Client</h3>
                        <p>Downloaded key file needs to be opened in the appropriate VPN client.</p>
                        <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID) { %>
                        <ul><li>
                            <a href="https://play.google.com/store/apps/details?id=net.openvpn.openvpn"
                               rel="nofollow" target="_blank">OpenVPN Connect</a> for Android.
                        </li></ul>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.IOS) { %>
                        <ul><li>
                            <a href="https://itunes.apple.com/us/app/openvpn-connect/id590379981?mt=8"
                               rel="nofollow" target="_blank">OpenVPN Connect</a> for iOS.
                        </li></ul>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS) { %>
                        <ul><li>
                            <a href="https://openvpn.net"
                               rel="nofollow" target="_blank">OpenVPN</a> client for Windows.
                        </li></ul>

                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.LINUX) { %>
                        <ul><li>
                            <a href="https://openvpn.net"
                               rel="nofollow" target="_blank">OpenVPN</a> client for Linux.
                        </li></ul>

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
                        <ul><li>
                            <a href="https://tunnelblick.net/"
                               rel="nofollow" target="_blank">Tunnelblick</a> for MAC OS.
                        </li></ul>

                        <% } %>
                    </div>
                </div>
            <% } %>

            <% if (vpnBean.getLandingLink() != null) { %>
                <div class="row">
                    <div class="col-sm-12">
                        <h3>Device transfer</h3>
                        <p>
                            In case this link is displayed on a different device it belongs to below is a QR code
                            with the link to scan.
                        </p>

                        <div class="qrWrap">
                            <div id="qrcode" class="qr"></div>
                        </div>
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
            <div id="divError" class="alert alert-danger">
                <strong>Error:</strong>
                <% if (vpnBean.getLinkError() == VpnLinkError.OTP_INVALID) { %>
                Link is invalid
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_OLD) { %>
                Link is too old
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_TOO_MANY) { %>
                Link used too many times
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_COOKIE) { %>
                Link already used on a different device
                <% } else if (vpnBean.getLinkError() == VpnLinkError.OTP_DESCRIPTOR) { %>
                Link already used on a different device / address
                <% } else if (vpnBean.getLinkError() == VpnLinkError.NO_CONFIGURATION) { %>
                Configuration has already been downloaded
                <% } else if (vpnBean.getLinkError() == VpnLinkError.GENERIC) { %>
                Link is invalid, generic error
                <% } else { %>
                No link given
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

