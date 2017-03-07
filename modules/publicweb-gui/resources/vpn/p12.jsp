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
            <div id="divStatusNotif" class="alert alert-success" style="display: none;">Thank you, you have successfully downloaded your administrator key.
                Please install the key.</div>

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
                        <h3>Administrator key</h3>
                        <ul>
                            <li>Private Space allows you to add keys to new users, as well as revoke keys not needed any more.</li>
                            <li>You need to install your administrator key before you can add new users. This key ensures that no-one else can access your Private Space.</li>
                        </ul>
                    </div>
                </div>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>How To Install The Key</h3>
                        <ol>
                            <li>You need two piece of information to install your administrator key. These are delivered with different channels for maximum security.</li>
                            <li>First item is an installation password. You will find this under your account in
                            <a href="https://enigmabridge.freshdesk.com" rel="nofollow" target="_blank"> our support system</a>.</li>

                            <li>Second item is the key itself, which you download by clicking the "Download Key" button above.</li>
                            <li>To start the key installation, please click (or double-click) the key file you downloaded here (file name "${p12Bean.p12FileName}").
                                Follow instructions and use the password when it is requested.</li>
                            <li>Once the key is installed proceed to the <a href="${p12Bean.indexLink}" target="_blank">next step</a>.</li>
                        </ol>
                    </div>
                </div>
            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Connected to Private Space</h3>
                    <ul>
                        <li>Connection clients use the technology called "VPN" for providing the required level of control and security of your Private Space.</li>
                        <li>Please have a look at <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank">our user manuals</a> for more information, examples, and videos how to use recommended connection clients.</li>
                    </ul>
                </div>
            </div>

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
                The administrator key is not available.
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_OLD) { %>
                The administrator key is not available (the link is too old)
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_TOO_MANY) { %>
                The administrator key is not available (the link was already used).
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_COOKIE) { %>
                The administrator key is not available (the link has been already used on a different device).
                <% } else if (p12Bean.getLinkError() == VpnLinkError.OTP_DESCRIPTOR) { %>
                The administrator key is not available  (the link has been already used on a different device / address).
                <% } else if (p12Bean.getLinkError() == VpnLinkError.NO_CONFIGURATION) { %>
                The administrator key is not available (the configuration has already been downloaded).
                <% } else if (p12Bean.getLinkError() == VpnLinkError.GENERIC) { %>
                The administrator key is not available (the link is invalid, generic error).
                <% } else if (p12Bean.getLinkError() == VpnLinkError.NOT_IN_VPN) { %>
                The administrator key is not available. The link can be used only when connected to the Private Space.
                <% } else { %>
                The administrator key is not available. No link given.
                <% } %>
            </div>

            <div class="row">
                <div class="col-sm-12">
                    <h3>The administrator key is not available</h3>
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

