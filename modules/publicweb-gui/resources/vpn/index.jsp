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

<jsp:useBean id="indexBean" class="org.ejbca.ui.web.pub.vpn.IndexBean" scope="session" />
<%
    indexBean.initialize(request, response);
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
        <link rel="stylesheet" href="../scripts/font-awesome.min.css" type="text/css" />
        <script type="text/javascript" src="../scripts/jquery-1.12.4.min.js"></script>
        <script type="text/javascript" src="../scripts/jquery.qrcode.min.js"></script>
        <script type="text/javascript" src="../scripts/bootstrap-3.3.7.min.js"></script>
        <script type="text/javascript" src="../scripts/vpnfunctions.js"></script>
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
        <p>${indexBean.hostname}</p>
    </div>

    <div class="container">
        <c:choose>

            <%-- VPN has not been downloaded yet --%>
            <c:when test="${indexBean.onlyAdmin && !indexBean.vpnDownloaded}">
                <div id="divError" class="alert alert-warning">
                    Private Space key has not been downloaded yet
                </div>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Private Space key is required</h3>
                        <p>
                            <ul>
                                <li>In order to continue you need to download a Private Space Key.</li>
                                <li>The Private Space Key is used to connect to the VPN.</li>
                                <li>Link for the Private Space Key should be already in your mailbox.</li>
                            </ul>
                        </p>
                    </div>
                </div>
            </c:when>

            <%--P12 file has not been downloaded yet--%>
            <c:when test="${indexBean.onlyAdmin && indexBean.isAdminP12Available && indexBean.vpnDownloaded && !indexBean.connectedFromVpn}">
                <div id="divError" class="alert alert-warning">
                    You are not connected via Private Space VPN
                </div>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>You need to be connected via Private Space VPN to continue</h3>
                        <p>
                            <ul>
                                <li>Administrator key has not been downloaded yet.</li>
                                <li>In order to download the administrator key you need to connect via Private Space VPN.</li>
                                <li>Open the Private Space key you downloaded already to connect to the Private Space VPN and refresh this page.</li>
                            </ul>
                        </p>
                    </div>
                </div>
            </c:when>

            <%--P12 downloaded, user did not login to the admin page --%>
            <c:otherwise>


                <h2 style="color:#00a7d7">Add users and devices to your Private Space</h2>
                <div class="row">
                    <div class="col-md-2">
                        <p>
                            <a href="${indexBean.adminPageLink}" class="btn btn-auto btn-warning">
                                <i class="fa fa-flag-checkered fa-5x"></i><br>
                                First Time Here<br>Click When Ready
                            </a>
                        </p>

                    </div>
                    <div class="col-md-10">
                        <ul>
                            <li>As you are here for the first time, we are just checking you installed your admin key.</li>
                            <li>Installation of the admin key requires:</li>
                            <ul>
                                <li>The key itself. It is somewhere on your computer, stored as "${indexBean.spaceName}-superadmin.p12".
                                </li>
                                <li>A password, which you can find in an email with Service installation status, sent by Enigma
                                    Bridge Ltd.
                                </li>
                            </ul>
                            <li>Open the key and follow the import procedure. Keep the default options and when asked, enter the
                                password.
                            </li>
                            <li>When you complete the installation, you may delete the original key.</li>
                        </ul>
                    </div>
                </div>


                <div class="row">
                    <div class="col-md-2">
                        &nbsp;
                    </div>
                    <div class="col-md-10">
                        <br>
                        <div style="text-align: center;">
                            <a href="https://enigmabridge.freshdesk.com/helpdesk/tickets/new" class="btn btn-auto btn-info">
                                <i class="fa fa-bell-o fa-2x"></i><br>
                                Ask for help if things go wrong
                            </a>
                        </div>
                    </div>
                </div>

            </c:otherwise>
        </c:choose>

        </div>

    </body>
    </html>
</f:view>


