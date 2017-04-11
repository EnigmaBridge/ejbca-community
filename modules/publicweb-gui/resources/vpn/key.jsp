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
    int curstep = 1;

    // Init OTP check
    vpnBean.isOtpValid();
%>
<f:view>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=<%= org.ejbca.config.WebConfiguration.getWebContentEncoding() %>" />
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=0">
    <title><%= org.ejbca.core.ejb.vpn.VpnConfig.getConfigDownloadTitle() %> - <%= org.ejbca.core.ejb.vpn.VpnConfig.getServerHostname() %></title>
    <link rel="shortcut icon" href="../images/favicon-eb.png" type="image/png" />
    <link rel="stylesheet" href="../scripts/bootstrap-3.3.7.min.css" type="text/css" />
    <link rel="stylesheet" href="../scripts/checkbox-x.min.css" media="all" type="text/css" />
    <link rel="stylesheet" href="../scripts/theme-krajee-flatblue.min.css" media="all" type="text/css" />
    <link rel="stylesheet" href="../scripts/vpnstyle.css" type="text/css" />
    <script type="text/javascript" src="../scripts/jquery-1.12.4.min.js"></script>
    <script type="text/javascript" src="../scripts/jquery.qrcode.min.js"></script>
    <script type="text/javascript" src="../scripts/jquery.visible.min.js"></script>
    <script type="text/javascript" src="../scripts/bootstrap-3.3.7.min.js"></script>
    <script type="text/javascript" src="../scripts/vpnfunctions.js"></script>
    <script type="text/javascript" src="../scripts/checkbox-x.min.js" ></script>
    <script type="text/javascript">
        var installedCheck;
        var downloadButton;

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

            <% if (vpnBean.isOtpValid()) { %>
            downloadButton = $("#btnDownload");
            installedCheck = $("#check-installed");
            installedCheck.checkboxX({threeState: false, size:'xl'});
            installedCheck.on('change', function() {
                var checked = installedCheck.is(':checked');
                downloadButton.prop('disabled', !checked);
                if (checked) {
                    downloadButton.removeClass('disabled');
                    downloadButton.show('slow');
                    scrollToIfNotVisible(downloadButton);
                } else {
                    downloadButton.addClass('disabled');
                    downloadButton.hide('slow');
                }
            });
            <% } %>
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
        <c:when test="${vpnBean.otpValid || vpnBean.otpAlreadyDownloaded}">

            <h:panelGroup layout="block" rendered="#{vpnBean.vpnUser != null}">
            <div id="vpnInfo" class="form-group" style="display: block;">
                <div class="panel panel-default">
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
            </h:panelGroup>

            <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID
                    || vpnBean.getOsGroup() == OperatingSystem.IOS
                    || vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

            <%-- Extra download step - needed for mobile devices - and linux (package manager) --%>
            <% if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>
            <div class="row">
                <div class="col-sm-12">
                    <h3>Step 1 - Download a connection client</h3>
                    <% curstep +=1; %>

                    <div class="form-group">
                        <div id="divButtonClientDownload">
                            <a class="btn btn-primary btn-xl btn-block btn-wrap" id="btnClientDownload" rel="nofollow"
                            <% if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS) { %>
                               href="https://privatespace-deploy.enigmabridge.com/vpnclient/openvpn-latest.exe">Download OpenVPN Client</a>
                            <% } else if (vpnBean.getOsGroup() == OperatingSystem.LINUX) { %>
                                href="https://openvpn.net/index.php/open-source/downloads.html" target="_blank">Download OpenVPN Client</a>
                            <% } else if (vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>
                                href="https://privatespace-deploy.enigmabridge.com/vpnclient/tunnelblick-latest.dmg">Download Tunnelblick Client</a>
                            <% } %>
                        </div>
                    </div>

                </div>
            </div>
            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Step <%=curstep%> - Install the connection client</h3>
                    <% curstep +=1; %>

                    <%-- For mobile devices - download is together with install --%>
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

                    <%-- Desktop platforms below - more instructions --%>
                    <% } else if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS) { %>
                        <p>
                            Click the client icon in your Downloads folder to install.
                            Follow instructions and re-confirm the install request if asked.
                        </p>

                        <p>
                            <img src="../images/openvpn-down.png" alt="OpenVPN downloaded icon">
                        </p>

                        <p>
                            If you need more help, please use our <a href="https://openvpn.net/index.php/access-server/docs/admin-guides-sp-859543150/howto-connect-client-configuration/395-how-to-install-the-openvpn-client-on-windows.html">installation guides</a>.
                        </p>

                    <% } else if (vpnBean.getOsGroup() == OperatingSystem.LINUX) { %>
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
                            Click the client icon in your Downloads folder to install.
                            Follow instructions and re-confirm the install request if asked
                        </p>

                    <% } %>
                </div>
            </div>

            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Step <%=curstep%> - Download your key</h3>
                    <% curstep +=1; %>

                    <%-- Download link is still valid --%>
                    <% if (vpnBean.isOtpValid()) { %>
                    <p>Download your private space key to your device.</p>

                    <%-- General checkbox for all platforms --%>
                    <div id="divStatusClient" class="alert alert-info">
                        <h:selectBooleanCheckbox value="#{vpnBean.clientInstalledCheck}" id="check-installed"
                                                 styleClass="cbx-krajee-flatblue"/>
                        <label for="check-installed">I have the connection client installed</label>
                    </div>
                    <script type="text/javascript">
                        $(function() {
                            downloadButton.prop('disabled', true);
                            downloadButton.addClass('disabled');
                        });
                    </script>

                    <div id="divStatusNotif" class="alert alert-success" style="display: none;">
                        You will now need to open the downloaded key in your connection client.
                        Proceed to the next step.
                    </div>

                    <div class="form-group">
                        <div id="divButtonDownload">
                            <a class="btn btn-primary btn-xl btn-block btn-wrap" id="btnDownload" style="display: none"
                               href="${vpnBean.downloadLink}">Download Key</a>
                        </div>
                    </div>

                    <% } else { %>
                        <%-- Download link is invalid - show information --%>
                        <div class="form-group">
                            <div id="divStatusNotif2" class="alert alert-info">
                                The Private Space key was already downloaded. Proceed to the next step.
                            </div>
                        </div>

                    <% } %>

                </div>
            </div>

            <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID
                    || vpnBean.getOsGroup() == OperatingSystem.IOS
                    || vpnBean.getOsGroup() == OperatingSystem.WINDOWS
                    || vpnBean.getOsGroup() == OperatingSystem.LINUX
                    || vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>

                <div class="row">
                    <div class="col-sm-12">
                        <h3>Step <%=curstep%> - Import your key to the client</h3>
                        <% curstep +=1; %>

                        <%-- For mobile devices - download is together with install --%>
                        <% if (vpnBean.getOsGroup() == OperatingSystem.ANDROID) { %>
                            <p>
                                <ul>
                                    <li>Open the downloaded Private Space Key in your browser.</li>
                                    <li>Import the key in the OpenVPN Connect client.</li>
                                </ul>
                            </p>


                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.IOS) { %>
                            <p>
                                <ul>
                                    <li>Open the downloaded key in the OpenVPN Connect from the browser.</li>
                                </ul>
                            </p>


                        <%-- Desktop platforms below - more instructions --%>
                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.WINDOWS) { %>
                            <div class="form-group">
                                <ul>
                                    <li>Start the client if not running yet.</li>
                                    <li>Find its icon in the task bar in bottom right corner and right click it to select your key's configuration file.</li>
                                    <li>Once done, click on "Connect". The icon will turn green.</li>
                                </ul>
                            </div>

                            <div class="form-group">
                                <iframe src="https://player.vimeo.com/video/212612132" width="800" height="450" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
                            </div>


                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.LINUX) { %>
                            <p>
                                <ul>
                                    <li>Copy the private space key (VPN config) to the OpenVPN Connect config directory.</li>
                                    <li>Or import the key to the OpenVPN Connect directly.</li>
                                </ul>
                            </p>


                        <% } else if (vpnBean.getOsGroup() == OperatingSystem.MAC_OS_X) { %>
                            <p>
                                <ul>
                                    <li>Double click the private space key to import it to the Tunnelblick.</li>
                                    <li>Click on Tunnelblick icon in task bar, connect to the private space.</li>
                                </ul>
                            </p>


                        <% } %>
                    </div>
                </div>

            <% } %>

            <div class="row">
                <div class="col-sm-12">
                    <h3>Step <%=curstep%> - Open your Private Space</h3>
                    <% curstep +=1; %>
                    <p>
                        Navigate your browser to <a href="http://private.space">http://private.space</a>.
                        If all steps were done correctly you will see Private Space intro page.
                    </p>
                </div>
            </div>

            <hr />
                
            <div class="row">
                <div class="col-sm-12">
                    <h3>Troubleshooting</h3>
                    <p>
                        <ul>
                            <li>Please have a look at <a href="https://enigmabridge.freshdesk.com/solution/categories/19000098261" rel="nofollow" target="_blank">our user manuals</a> for more information, examples, and videos how to use recommended connection clients.</li>
                            <li>In case of a problem please consult your administrator. Alternatively, feel free to <a href="https://enigmabridge.freshdesk.com/helpdesk/tickets/new">request help from Enigma Bridge</a>.</li>
                            <li>Connection clients use the technology called "VPN" for providing the required level of control and security of your Private Space.</li>
                        </ul>
                    </p>
                </div>
            </div>

            <% if (vpnBean.getLandingLink() != null) { %>
                <div class="row">
                    <div class="col-sm-12">
                        <h3>Key transfer</h3>
                        <p>
                            If the key is for a device able to read QR codes, you can use the code below.
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

