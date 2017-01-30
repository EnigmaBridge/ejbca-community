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
<%
	GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, CryptoTokenRules.BASE.resource());
%>
<%!
	static final String USER_PARAMETER           = "username";
	static final String HIDDEN_USERNAME          = "hiddenusername";
	static final String HIDDEN_RECORDNUMBER      = "hiddenrecordnumber";
	static final String SELECT_REVOKE_REASON     = "selectrevokereason";
%>

<%
	// TODO: fix resource for ACL
	final String VIEWCERT_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "viewcertificate.jsp";
	final String VIEWUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/viewendentity.jsp";
	final String EDITUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/editendentity.jsp";
	final String VIEWHISTORY_LINK         = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/viewhistory.jsp";
	final String VIEWTOKEN_LINK           = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "hardtoken/viewtoken.jsp";
%>
<jsp:useBean id="vpnUsersMBean" class="org.ejbca.ui.web.admin.vpn.VpnUsersMBean" scope="session" />

<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <link rel="stylesheet" type="text/css" href="<%= globalconfiguration.getAdminWebPath() %>scripts/vpnstyle.css"/>
  <script src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery-2.1.0.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/jquery.qrcode.min.js"></script>
  <script src="<%= globalconfiguration.getAdminWebPath() %>scripts/vpn.js"></script>

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
            regenerateQrCode('${!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUser.otpDownloadLink != null ? vpnUsersMBean.currentVpnUser.otpDownloadLink : ""}');
        });
	</script>
</head>
<body>
	<h1>
	    <h:outputText value="#{web.text.VPNUSER_NEW}" rendered="#{vpnUsersMBean.currentVpnUserId == null}"/>
		<h:outputText value="#{web.text.VPNUSER_HEAD} #{vpnUsersMBean.currentVpnUser.name}" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>
	</h1>
	<div class="message"><h:messages layout="table" errorClass="alert" infoClass="info"/></div>
	<h:form id="currentVpnUserForm">
	<h:panelGrid columns="2">
		<h:panelGroup>
			<h:outputLink rendered="#{vpnUsersMBean.paramRef eq 'default'}" value="adminweb/vpn/vpnusers.jsf"><h:outputText value="#{web.text.VPNUSER_NAV_BACK}"/></h:outputLink>
			<h:outputLink rendered="#{vpnUsersMBean.paramRef eq 'caactivation'}" value="adminweb/ca/caactivation.jsf"><h:outputText value="#{web.text.CRYPTOTOKEN_NAV_BACK_ACT}"/></h:outputLink>
		</h:panelGroup>
		<h:commandButton action="#{vpnUsersMBean.toggleCurrentCryptoTokenEditMode}" value="#{web.text.VPNUSER_NAV_EDIT}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.allowedToModify}"/>
		<h:panelGroup id="placeholder1" rendered="#{vpnUsersMBean.currentVpnUserEditMode || !vpnUsersMBean.allowedToModify}"/>

		<h:outputLabel for="currentVpnUserId" value="#{web.text.CRYPTOTOKEN_ID}:" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>
		<h:outputText id="currentVpnUserId" value="#{vpnUsersMBean.currentVpnUserId}" rendered="#{vpnUsersMBean.currentVpnUserId != null}"/>

		<h:outputLabel for="currentVpnUserName" value="#{web.text.VPNUSER_NAME}:" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>
		<h:outputText id="currentVpnUserName" value="#{vpnUsersMBean.currentVpnUser.name}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode}"/>

		<h:outputLabel for="currentVpnUserEmail" value="#{web.text.VPNUSER_EMAIL}:"/>
		<h:panelGroup id="currentVpnUserEmail">
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.email}" style="width: 300px" required="true"
						 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}">
	    		<f:validator validatorId="emailValidator"/>
	    	</h:inputText>
	    	<h:outputText value="#{vpnUsersMBean.currentVpnUser.email}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode || vpnUsersMBean.currentVpnUserId != null}"/>
		</h:panelGroup>

		<h:outputLabel for="currentVpnUserDevice" value="#{web.text.VPNUSER_DEVICE}:"/>
		<h:panelGroup id="currentVpnUserDevice">
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.device}" style="width: 300px" required="true"
						 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}">
	    		<f:validator validatorId="legalCharsValidator"/>
	    	</h:inputText>
	    	<h:outputText value="#{vpnUsersMBean.currentVpnUser.device}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode || vpnUsersMBean.currentVpnUserId != null}"/>
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

		<h:outputLabel for="sendEmailNowAfterCreateCheck" value="#{web.text.VPNUSER_MAIL_SEND_CHECKBOX}" rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"/>
		<h:selectBooleanCheckbox id="sendEmailNowAfterCreateCheck" value="#{vpnUsersMBean.currentVpnUser.sendConfigEmail}"
								 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}"/>

		<h:panelGroup/>
		<h:panelGroup>
			<h:commandButton action="#{vpnUsersMBean.cancelEdit}" value="#{web.text.CRYPTOTOKEN_CANCEL}"
							 rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId != null}"/>
			<h:commandButton action="#{vpnUsersMBean.saveCurrentVpnUser}" value="#{web.text.CRYPTOTOKEN_SAVE}"
							 rendered="#{vpnUsersMBean.currentVpnUserEditMode}" onclick="return bodyProgress(true)"/>
		</h:panelGroup>
	</h:panelGrid>
	</h:form>

	<div class="qrWrap">
		<div id="qrcode" class="qr"></div>
	</div>

	<div class="modal">
		<div class="modal-wrap"></div>
	</div>

	<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />
</body>
</f:view>
</html>
