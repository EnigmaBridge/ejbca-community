<%
/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

 // Author: ph4r05
%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page pageEncoding="UTF-8"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="
org.ejbca.ui.web.admin.configuration.EjbcaWebBean,
org.ejbca.config.GlobalConfiguration,
org.ejbca.core.model.authorization.AccessRulesConstants,
org.cesecore.authorization.control.AuditLogRules,
org.cesecore.authorization.control.CryptoTokenRules
"%>
<%@ page import="static org.ejbca.ui.web.admin.rainterface.ViewEndEntityHelper.USER_PARAMETER" %>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="vpnUsersMBean" scope="session" class="org.ejbca.ui.web.admin.vpn.VpnUsersMBean" />
<%
	GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, CryptoTokenRules.BASE.resource());
%>
<%!
	static final String USER_PARAMETER           = "username";
	static final String SELECT_REVOKE_REASON     = "selectrevokereason";
%>

<%
	final String VIEWCERT_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "viewcertificate.jsp";
	final String VIEWUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/viewendentity.jsp";
	final String JS_LINK                  = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "/vpn/js.jsf";
%>
<c:set var="SHOW_FIRST_ROW" value="#{vpnUsersMBean.paramRef eq 'default'}" />

<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /> - <%= org.ejbca.core.ejb.vpn.VpnConfig.getServerHostname() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="shortcut icon" href="<%= globalconfiguration.getAdminWebPath() %>images/favicon-eb.png" type="image/png" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <link rel="stylesheet" type="text/css" href="<%= globalconfiguration.getAdminWebPath() %>scripts/vpnstyle.css"/>
  <link rel="stylesheet" type="text/css" href="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery-ui-1.12.1.min.css"/>
  <script src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery-2.1.0.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery.qrcode.min.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery-ui-1.12.1.min.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/vpn.js"></script>

<% if (!vpnUsersMBean.getEjbcaMode()) { %>
  <link rel="stylesheet" href="<%= globalconfiguration.getAdminWebPath() %>scripts/bootstrap-3.3.7.min.css" type="text/css" />
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/bootstrap-3.3.7.min.js"></script>
<% } %>

	<script>
        function viewuser(username){
            var link = "<%= VIEWUSER_LINK %>?<%= USER_PARAMETER %>="+username;
            link = encodeURI(link);
            win_popup = window.open(link, 'view_user','height=650,width=750,scrollbars=yes,toolbar=no,resizable=1');
            win_popup.focus();
            return false;
        }

        function viewcert(username){
            var link = "<%= VIEWCERT_LINK %>?<%= USER_PARAMETER %>="+username;
            link = encodeURI(link);
            win_popup = window.open(link, 'view_cert','height=650,width=750,scrollbars=yes,toolbar=no,resizable=1');
            win_popup.focus();
            return false;
        }

        function regenerateQrCode(link){
            var wrapper = $('#qrWrap');
            var divQrCode = $('#qrcode');
            divQrCode.html("");

            if (!link || 0 === link.length) {
                wrapper.hide();
                return;
            }

            // If already rendered from QR code, generate no more
            // Would overwrite QR nonce
            var qr_nonce = findGetParameter('qrnonce');
            if (qr_nonce){
                console.log('QRnonce present: ' + qr_nonce);
                wrapper.hide();
                return;
            }

			<% if (!vpnUsersMBean.isCurrentVpnUserEditMode()) { %>
            // Get QR nonce async.
            $.getJSON('<%=JS_LINK%>?json=qr')
                .done(function(data) {
                    try{
                        var nonce = data['nonce'];
                        var otp = data['otp'];
                        if (otp !== '${vpnUsersMBean.currentVpnUser.otpDownload}'){
                            console.log('OTP nonce invalid');
                            wrapper.hide();
                            return;
                        }

                        wrapper.show();
                        var qrCodeSettings = {
                            "render": "canvas",
                            "text": link + '&qrnonce=' + nonce,
                            "size": 300
                        };
                        divQrCode.qrcode(qrCodeSettings);
                    } catch (e){
                        console.log(e);
                    }
                });
            <% } %>
        }

        $(function() {
            regenerateQrCode('${!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDownloadLink != null ? vpnUsersMBean.currentVpnUser.otpDownloadLink : ""}');

            <% if (vpnUsersMBean.isCurrentVpnUserEditMode()) { %>
            $( ".userEmailField" ).autocomplete({
				'source': function(request, response) {
                    $.getJSON('<%=JS_LINK%>?json=ac-email&term=' + request['term'])
                        .done(function (data) {
                            response(data['result']);
                        });
                }
			});

            $( ".userDeviceField" ).autocomplete({
				'source': function(request, response) {
                    $.getJSON('<%=JS_LINK%>?json=ac-device&term=' + request['term'])
                        .done(function (data) {
                            response(data['result']);
                        });
                }
			});
			<% } %>
        });
	</script>
</head>
<body class="enigmabridge vpnuser <%= vpnUsersMBean.getEjbcaMode() ? "eb-ejbca" : "eb-solo" %>">

<% if (!vpnUsersMBean.getEjbcaMode()) { %>
<div class="navbar">
	<div class="container">
		<div class="navbar-header">
			<a class="navbar-brand" href="https://enigmabridge.com"></a>
		</div>
	</div>
</div>

<div class="jumbotron text-center">
	<h1>Private Space Administration</h1>
	<p><%= org.ejbca.core.ejb.vpn.VpnConfig.getServerHostname() %></p>
</div>

<div class="container eb-main-body">
<% } %>

	<h1>
	    <h:outputText value="#{web.text.VPNUSER_NEW}" rendered="#{vpnUsersMBean.currentVpnUserId == null}"/>
		<h:outputText value="#{web.text.VPNUSER_HEAD} #{vpnUsersMBean.currentVpnUser.name}" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>
	</h1>
	<div class="message"><h:messages layout="table" errorClass="alert" infoClass="info"/></div>
	<h:form id="currentVpnUserForm">

	<div class="panel panel-default">
	<div class="panel-body" id="pre-info">

	<h:panelGrid columns="2" styleClass="table table-vpn">
		<h:panelGroup rendered="#{SHOW_FIRST_ROW}">
			<h:outputLink rendered="#{vpnUsersMBean.paramRef eq 'default'}" value="adminweb/vpn/vpnusers.jsf?ejbcaMode=#{vpnUsersMBean.getEjbcaMode() ? 1 : 0}">
				<h:outputText value="#{web.text.VPNUSER_NAV_BACK}"/></h:outputLink>
		</h:panelGroup>

		<%-- Edit mode disabled for now. When enabled, modify SHOW_FIRST_ROW appropriatelly --%>
		<%--<h:commandButton action="#{vpnUsersMBean.toggleCurrentVpnUserEditMode}" value="#{web.text.VPNUSER_NAV_EDIT}" --%>
						 <%--rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.allowedToModify}"/>--%>
		<%--<h:panelGroup id="placeholder1" rendered="#{vpnUsersMBean.currentVpnUserEditMode || !vpnUsersMBean.allowedToModify}"/>--%>
		<h:panelGroup id="placeholder1" rendered="#{SHOW_FIRST_ROW}"/>

		<%-- ID field - commented out --%>
		<%--<h:outputLabel for="currentVpnUserId" value="#{web.text.CRYPTOTOKEN_ID}:" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>--%>
		<%--<h:outputText id="currentVpnUserId" value="#{vpnUsersMBean.currentVpnUserId}" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>--%>

		<h:outputLabel for="currentVpnUserName" value="#{web.text.VPNUSER_NAME}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:outputText id="currentVpnUserName" value="#{vpnUsersMBean.currentVpnUser.name}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>

		<h:outputLabel for="currentVpnUserEmail" value="#{web.text.VPNUSER_EMAIL}:" rendered="#{vpnUsersMBean.currentVpnUserEditMode}" />
		<h:panelGroup id="currentVpnUserEmail" rendered="#{vpnUsersMBean.currentVpnUserEditMode}">
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.email}" style="width: 300px" required="true"
						 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"
						 styleClass="userEmailField">
	    		<f:validator validatorId="emailValidator"/>
	    	</h:inputText>
		</h:panelGroup>

		<h:outputLabel for="currentVpnUserDevice" value="#{web.text.VPNUSER_DEVICE}:" rendered="#{vpnUsersMBean.currentVpnUserEditMode}" />
		<h:panelGroup id="currentVpnUserDevice" rendered="#{vpnUsersMBean.currentVpnUserEditMode}">
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.device}" style="width: 300px" required="true"
						 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"
						 styleClass="userDeviceField">
	    		<f:validator validatorId="legalCharsValidator"/>
	    	</h:inputText>
		</h:panelGroup>

		<h:outputLabel for="dateCreated" value="#{web.text.VPNUSER_DATE_CREATED}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
			<h:outputText id="dateCreated" value="#{vpnUsersMBean.currentVpnUser.dateCreatedDate}"
						  rendered="#{!vpnUsersMBean.currentVpnUserEditMode}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>

		<h:outputLabel for="dateModified" value="#{web.text.VPNUSER_DATE_MODIFIED}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
			<h:outputText id="dateModified" value="#{vpnUsersMBean.currentVpnUser.dateModifiedDate}"
						  rendered="#{!vpnUsersMBean.currentVpnUserEditMode}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>

		<h:outputLabel for="mailSentDateGrp" value="#{web.text.VPNUSER_MAIL_SENT}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:panelGroup id="mailSentDateGrp" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}">
			<h:outputText value="#{web.text.VPNUSER_DASH}" rendered="#{vpnUsersMBean.currentVpnUser.dateMailSent == null}"/>
			<h:outputText id="mailSentDate" value="#{vpnUsersMBean.currentVpnUser.dateMailSent}"
						  rendered="#{vpnUsersMBean.currentVpnUser.dateMailSent != null}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>
		</h:panelGroup>

		<h:outputLabel for="dateOtpDownloadedGrp" value="#{web.text.VPNUSER_OTP_USED}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:panelGroup id="dateOtpDownloadedGrp" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}">
			<h:outputText value="#{web.text.VPNUSER_DASH}" rendered="#{vpnUsersMBean.currentVpnUser.dateOtpDownloaded == null}"/>
			<h:outputText id="dateOtpDownloaded" value="#{vpnUsersMBean.currentVpnUser.dateOtpDownloaded}"
						  rendered="#{vpnUsersMBean.currentVpnUser.dateOtpDownloaded != null}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>
		</h:panelGroup>

		<h:outputLabel for="certificateId" value="#{web.text.VPNUSER_CERTIFICATE_ID}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:outputLink value="" id="certificateId" onclick="return viewcert('#{vpnUsersMBean.currentVpnUser.name}')"
					  rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.certificateId!=null}">
			<h:outputText value="#{vpnUsersMBean.currentVpnUser.certificateId}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		</h:outputLink>

		<h:outputLabel for="downloadLink" value="#{web.text.VPNUSER_DOWNLOAD_LINK}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDownloadLink != null}"/>
		<h:outputLink value="#{vpnUsersMBean.currentVpnUser.otpDownloadLink}" id="downloadLink" target="_blank"
					  rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDownloadLink != null}">
			<h:outputText value="#{vpnUsersMBean.currentVpnUser.otpDownloadLink}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDownloadLink != null}"/>
		</h:outputLink>

		<h:outputLabel for="directLink" value="#{web.text.VPNUSER_SERVER_LINK}:"
					   rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDirectLink != null}"/>
		<h:outputText id="directLink" value="#{vpnUsersMBean.currentVpnUser.otpDirectLink}"
					  rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDirectLink != null}"/>

		<h:outputLabel for="sendEmailNowAfterCreateCheck" value="#{web.text.VPNUSER_MAIL_SEND_CHECKBOX}" rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"/>
		<h:selectBooleanCheckbox id="sendEmailNowAfterCreateCheck" value="#{vpnUsersMBean.currentVpnUser.sendConfigEmail}"
								 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"/>

		<h:panelGroup rendered="#{vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:panelGroup rendered="#{vpnUsersMBean.currentVpnUserEditMode}">
			<h:commandButton action="#{vpnUsersMBean.cancelEdit}" value="#{web.text.CRYPTOTOKEN_CANCEL}"
							 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId != null}"/>
			<h:commandButton action="#{vpnUsersMBean.saveCurrentVpnUser}" value="#{web.text.CRYPTOTOKEN_SAVE}"
							 rendered="#{vpnUsersMBean.currentVpnUserEditMode}" onclick="return bodyProgress(true)"/>
		</h:panelGroup>
	</h:panelGrid>

	</div>
	</div>

	<div id="qrWrap" class="qrWrap" style="display: none">
		<h4>Key transfer</h4>
		<p>If the key is for a device able to read QR codes, you can use the code below.</p>
		<div id="qrcode" class="qr"></div>
	</div>

	</h:form>

<% if (!vpnUsersMBean.getEjbcaMode()) { %>
	</div>
<% } %>

	<div class="modal">
		<div class="modal-wrap"></div>
	</div>

	<footer class="footer eb-footer">
		<div class="container nav navbar navbar-toggleable-md ">

			<%	// Include Footer
				String footurl = globalconfiguration.getFootBanner(); %>
			<jsp:include page="<%= footurl %>" />

			<%--<div class="collapse navbar-collapse" id="navbarCollapse">--%>
				<%--<ul class="nav navbar-nav mr-auto">--%>
					<%--<li class="nav-item">--%>
						<%--<a class="nav-link" href="<%= globalconfiguration.getAdminWebPath() %>">EJBCA Admin</a>--%>
					<%--</li>--%>
					<%--<li class="nav-item">--%>
						<%--<a class="nav-link" href="<%= org.ejbca.core.ejb.vpn.VpnConfig.getPublicPageLink() %>">EJBCA Public</a>--%>
					<%--</li>--%>
				<%--</ul>--%>
			<%--</div>--%>
		</div>
	</footer>

</body>
</f:view>
</html>
