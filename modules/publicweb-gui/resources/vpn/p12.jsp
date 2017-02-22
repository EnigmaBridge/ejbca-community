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

<jsp:useBean id="p12Bean" class="org.ejbca.ui.web.pub.vpn.P12Bean" scope="session" />
<%
    p12Bean.initialize(request);
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
                    $( "#divButtonUpload" ).hide( "slow" );
                })
                .fail(function () {
                });

            return false; //this is critical to stop the click event which will trigger a normal file download
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
    <p>Administrator key download</p>
</div>

<div class="container">
    <h:outputText value="" rendered="#{p12Bean.otpValid}"/>

    <c:choose>
        <c:when test="${p12Bean.otpValid}">
            <div id="vpnInfo" class="form-group" style="display: block;">
                <div class="panel panel-default">
                    <%--<div class="panel-heading">Private space user key</div>--%>
                    <div class="panel-body" id="pre-info">
                        <table class="table table-vpn">
                            <tr>
                                <th style="border-top: none">Space name</th>
                                <td style="border-top: none">${p12Bean.hostname}</td>
                            </tr>
                            <tr>
                                <th>User</th>
                                <td>${p12Bean.token.otpId}</td>
                            </tr>
                            <tr>
                                <th>Generated</th>
                                <td>
                                    <h:outputText id="dateGenerated" value="#{p12Bean.dateGenerated}">
                                        <f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
                                    </h:outputText>
                                </td>
                            </tr>
                        </table>
                </div>
            </div>

            <% if (p12Bean.getLinkError() == VpnLinkError.NOT_IN_VPN) { %>
            <div id="divError" class="alert alert-danger">
                <strong>Error: </strong> The key can be downloaded only when connected to VPN
            </div>
            <% } else if (p12Bean.getIsMobileDevice()) { %>
            <div id="divError" class="alert alert-danger">
                Please try on your laptop or desktop computer.
                We apologise for the inconvenience, but administrator keys can't be downloaded onto mobile devices.
            </div>
            <% } else { %>
            <div id="divStatusNotif" class="alert alert-success" style="display: none;">Download successful</div>
            <div id="divButtonUpload">
                <a class="btn btn-primary btn-xl btn-block btn-wrap" id="btnDownload"
                   href="${p12Bean.downloadLink}">Download Key</a>
            </div>
            <% }%>

            <% if (p12Bean.getOsGroup() == OperatingSystem.WINDOWS
                    || p12Bean.getOsGroup() == OperatingSystem.LINUX
                    || p12Bean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>How To Install The Key</h3>
                        <ul>
                            <li>You will need a password to install the key. </li>
                            <li>You can find it under your account in our
                                <a href="https://enigmabridge.freshdesk.com" rel="nofollow" target="_blank">support system</a>.</li>
                            <li>Once you have obtained the password, locate the downloaded key on your computer.</li>
                            <li>The browser will show it as the last downloaded file at the bottom of your screen, or in a list available via a button in the top right corner.</li>
                            <li>It's name looks like "${p12Bean.p12FileName}". Open the key by clicking on it and follow the dialog to complete installation.</li>
                            <li>When asked for a password, use the one located earlier.</li>
                        </ul>
                    </div>
                </div>
            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Troubleshooting</h3>
                    <p>
                        In case of a problem please consult your administrator.
                        You can also contact Enigma Bridge support at:
                        <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank">https://enigmabridge.freshdesk.com</a>
                    </p>
                </div>
            </div>
                
        </c:when>
        <c:otherwise>
            <div id="divError" class="alert alert-danger">
                <strong>Error:</strong>
                <% if (p12Bean.getLinkError() == VpnLinkError.OTP_INVALID) { %>
                The link is invalid
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_OLD) { %>
                The link is too old
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_TOO_MANY) { %>
                The link was used too many times
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_COOKIE) { %>
                The link has been already used on a different device
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_DESCRIPTOR) { %>
                The link has been already used on a different device / address
                <% } else if (p12Bean.getLinkError() == VpnLinkError.NO_CONFIGURATION) { %>
                The configuration has already been downloaded
                <% } else if (p12Bean.getLinkError() == VpnLinkError.GENERIC) { %>
                The link is invalid, generic error
                <% } else if (p12Bean.getLinkError() == VpnLinkError.NOT_IN_VPN) { %>
                The link can be used only when connected via VPN
                <% } else { %>
                No link given
                <% } %>
            </div>

            <div class="row">
                <div class="col-sm-12">
                    <h3>This link is invalid</h3>
                    <p>The download link is invalid and cannot be used. It may be expired or already downloaded. </p>
                    <p>Please contact your administrator to issue a new download link.</p>
                    <p>You can also contact Enigma Bridge support at:
                    <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank">https://enigmabridge.freshdesk.com</a>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

</body>
</html>
</f:view>

