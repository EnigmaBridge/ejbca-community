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
    <title><%= org.ejbca.core.ejb.vpn.VpnConfig.getConfigDownloadTitle() %> - <%= org.ejbca.core.ejb.vpn.VpnConfig.getServerHostname() %></title>
    <link rel="shortcut icon" href="../images/favicon-eb.png" type="image/png" />
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
                    $( "#divButtonDownload" ).hide( "slow" );
                    $( "#divStatusClient" ).hide( "slow" );
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
<body class="enigmabridge">

<div class="navbar">
    <div class="container">
        <div class="navbar-header">
            <a class="navbar-brand" href="https://enigmabridge.com"></a>
        </div>
    </div>
</div>

<div class="jumbotron text-center">
    <h1>Welcome to Private Space</h1>
    <p>Device key download</p>
</div>

<div class="container">
    <h:outputText value="" rendered="#{vpnBean.otpValid}"/>

    <c:choose>
        <c:when test="${vpnBean.otpValid}">
            <div id="vpnInfo" class="form-group" style="display: block;">
                <div class="panel panel-default">
                    <%--<div class="panel-heading">Private space user key</div>--%>
                    <div class="panel-body" id="pre-info">
                        <table class="table table-vpn">
                            <tr>
                                <th style="border-top: none">Space name</th>
                                <td style="border-top: none">${vpnBean.hostname}</td>
                            </tr>
                            <tr>
                                <th>Device</th>
                                <td>${vpnBean.vpnUser.email}/${vpnBean.vpnUser.device}</td>
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

            <div id="divStatusNotif" class="alert alert-success" style="display: none;">You will now need the recommended client (see below) to open the downloaded key. Then just click to connect and go to <a href="http://private.space">http://private.space</a>.</div>

            <div id="divStatusClient" class="alert alert-info">Make sure you have installed a connection client before downloading the key.</div>

            <div id="divButtonDownload">
                <a class="btn btn-primary btn-xl btn-block btn-wrap" id="btnDownload"
                   href="${vpnBean.downloadLink}">Download Key</a>
            </div>

            <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID
                    || vpnBean.getOsGroup() == OperatingSystem.IOS
                    || vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Recommended Client</h3>
                        <p>  Your browser will store the key on your computer.
                            You need to add it to a connection client (click/double-click on the key file).
                              We recommend:</p>
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
                               rel="nofollow" target="_blank">Tunnelblick</a> for Mac OS X.
                        </li></ul>

                        <% } %>
                    </div>
                </div>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Connected to Private Space</h3>
                        <ul>
                            <li>Connection clients use the technology called "VPN" for providing the required level of control and security of your Private Space.</li>
                            <li>Please have a look at <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank">our user manuals</a> for more information, examples, and videos how to use recommended connection clients.</li>
                        </ul>
                    </div>
                </div>

            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Troubleshooting</h3>
                    <p>
                        In case of a problem please consult your administrator.
                        Please have a look at <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank"> our user guides</a> if unsure.
                    </p>
                </div>
            </div>

            <% if (vpnBean.getLandingLink() != null) { %>
                <div class="row">
                    <div class="col-sm-12">
                        <h3>Key transfer</h3>
                        <p>
                            If the key is for another device able to read QR codes, you can use the code below.
                        </p>

                        <div class="qrWrap">
                            <div id="qrcode" class="qr"></div>
                        </div>
                    </div>
                </div>
            <% } %>
                
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
                    <h3>The private space key is not available</h3>
                    <p>The key download link is invalid and cannot be used.
                        It may be expired or already downloaded. </p>
                    <p>Please contact your administrator to issue a new key.</p>
                    <p>You can also contact Enigma Bridge support at:
                        <a href="https://enigmabridge.freshdesk.com">https://enigmabridge.freshdesk.com</a></p>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

</body>
</html>
</f:view>

