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

 // Version: $Id: cryptotoken.jsp 19577 2014-08-25 14:17:31Z davidcarella $
%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ page pageEncoding="UTF-8"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="
org.ejbca.ui.web.admin.configuration.EjbcaWebBean,
org.ejbca.config.GlobalConfiguration,
org.ejbca.core.model.authorization.AccessRulesConstants,
org.cesecore.authorization.control.AuditLogRules,
org.cesecore.authorization.control.CryptoTokenRules
"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<% GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, CryptoTokenRules.BASE.resource()); %>
<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <script src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
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
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.email}" style="width: 300px" rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}">
	    		<f:validator validatorId="legalCharsValidator"/>
	    	</h:inputText>
	    	<h:outputText value="#{vpnUsersMBean.currentVpnUser.email}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode || vpnUsersMBean.currentVpnUserId != null}"/>
		</h:panelGroup>

		<h:outputLabel for="currentVpnUserDevice" value="#{web.text.VPNUSER_DEVICE}:"/>
		<h:panelGroup id="currentVpnUserDevice">
	    	<h:inputText value="#{vpnUsersMBean.currentVpnUser.device}" style="width: 300px" rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId == null}">
	    		<f:validator validatorId="legalCharsValidator"/>
	    	</h:inputText>
	    	<h:outputText value="#{vpnUsersMBean.currentVpnUser.device}" rendered="#{!vpnUsersMBean.currentVpnUserEditMode || vpnUsersMBean.currentVpnUserId != null}"/>
		</h:panelGroup>

		<h:panelGroup/>
		<h:panelGroup>
			<h:commandButton action="#{vpnUsersMBean.cancelCurrentCryptoToken}" value="#{web.text.CRYPTOTOKEN_CANCEL}" rendered="#{vpnUsersMBean.currentVpnUserEditMode && vpnUsersMBean.currentVpnUserId != null}"/>
			<h:commandButton action="#{vpnUsersMBean.saveCurrentVpnUser}" value="#{web.text.CRYPTOTOKEN_SAVE}" rendered="#{vpnUsersMBean.currentVpnUserEditMode}"/>
		</h:panelGroup>
	</h:panelGrid>
	</h:form>

	<%--<h:outputText value="#{web.text.CRYPTOTOKEN_KPM_NA}" rendered="#{!vpnUsersMBean.currentVpnUser.active && vpnUsersMBean.currentVpnUserId!=null}"/>--%>
	<%--<h:form rendered="#{vpnUsersMBean.currentVpnUser.active}">--%>
	<%--<h:dataTable value="#{vpnUsersMBean.keyPairGuiList}" var="keyPairGuiInfo" rendered="#{!vpnUsersMBean.keyPairGuiListEmpty}"--%>
		<%--styleClass="grid" style="border-collapse: collapse; right: auto; left: auto">--%>
		<%--<h:column>--%>
			<%--<h:selectBooleanCheckbox value="#{keyPairGuiInfo.selected}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_KPM_ALIAS}"/></f:facet>--%>
			<%--<h:outputText value="#{keyPairGuiInfo.alias}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_KPM_ALGO}"/></f:facet>--%>
			<%--<h:outputText value="#{keyPairGuiInfo.keyAlgorithm}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_KPM_SPEC}"/></f:facet>--%>
			<%--<h:outputText value="#{keyPairGuiInfo.keySpecification}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_KPM_SKID}"/></f:facet>--%>
			<%--<h:outputText style="font-family: monospace;" value="#{keyPairGuiInfo.subjectKeyID}" rendered="#{!keyPairGuiInfo.placeholder}"/>--%>
			<%--<h:outputText value="#{web.text.CRYPTOTOKEN_KPM_NOTGENERATED}" rendered="#{keyPairGuiInfo.placeholder}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_KPM_ACTION}"/></f:facet>--%>
			<%--<h:commandButton value="#{web.text.CRYPTOTOKEN_KPM_TEST}" action="#{vpnUsersMBean.testKeyPair}" rendered="#{vpnUsersMBean.allowedToKeyTest}" disabled="#{keyPairGuiInfo.placeholder}"/>--%>
			<%--<h:commandButton value="#{web.text.CRYPTOTOKEN_KPM_REMOVE}" action="#{vpnUsersMBean.removeKeyPair}" rendered="#{vpnUsersMBean.allowedToKeyRemoval}"--%>
				<%--onclick="return confirm('#{web.text.CRYPTOTOKEN_KPM_CONF_REM}')"/>--%>
            <%--<h:commandButton value="#{web.text.CRYPTOTOKEN_KPM_GENFROMTEMPLATE}" action="#{vpnUsersMBean.generateFromTemplate}" rendered="#{keyPairGuiInfo.placeholder && vpnUsersMBean.allowedToKeyGeneration}"/>--%>
			<%--<h:outputLink value="adminweb/cryptoTokenDownloads?cryptoTokenId=#{vpnUsersMBean.currentVpnUserId}&alias=#{keyPairGuiInfo.alias}" rendered="#{!keyPairGuiInfo.placeholder}">--%>
				<%--<h:outputText value="#{web.text.CRYPTOTOKEN_KPM_DOWNPUB}"/>--%>
			<%--</h:outputLink>--%>
		<%--</h:column>--%>
	<%--</h:dataTable>--%>


	<%--<h:panelGroup rendered="#{vpnUsersMBean.keyPairGuiListFailed}">--%>
	    <%--<div class="message"><table><tr><td class="alert"><h:outputText value="#{vpnUsersMBean.keyPairGuiListError}"/></td></tr></table></div>--%>
    <%--</h:panelGroup>--%>
	<%--<h:outputText value="#{web.text.CRYPTOTOKEN_KPM_NOPAIRS}" rendered="#{vpnUsersMBean.keyPairGuiListEmpty && !vpnUsersMBean.keyPairGuiListFailed}"/>--%>
	<%--<h:panelGrid columns="3">--%>
		<%--<h:panelGroup rendered="#{!vpnUsersMBean.keyPairGuiListEmpty && vpnUsersMBean.allowedToKeyRemoval}"/>--%>
		<%--<h:panelGroup rendered="#{!vpnUsersMBean.keyPairGuiListEmpty && vpnUsersMBean.allowedToKeyRemoval}"/>--%>
	    <%--<h:commandButton value="#{web.text.CRYPTOTOKEN_KPM_REMOVESEL}" action="#{vpnUsersMBean.removeSelectedKeyPairs}"--%>
	    	<%--rendered="#{!vpnUsersMBean.keyPairGuiListEmpty && vpnUsersMBean.allowedToKeyRemoval}" onclick="return confirm('#{web.text.CRYPTOTOKEN_KPM_CONF_REMS}')"/>--%>
		<%--<h:inputText value="#{vpnUsersMBean.newKeyPairAlias}" rendered="#{vpnUsersMBean.allowedToKeyGeneration}">--%>
			<%--<f:validator validatorId="legalCharsValidator"/>--%>
		<%--</h:inputText>--%>
		<%--<h:selectOneMenu value="#{vpnUsersMBean.newKeyPairSpec}" rendered="#{vpnUsersMBean.allowedToKeyGeneration}">--%>
			<%--<f:selectItems value="#{vpnUsersMBean.availbleKeySpecs}"/>--%>
		<%--</h:selectOneMenu>--%>
	    <%--<h:commandButton value="#{web.text.CRYPTOTOKEN_KPM_GENNEW}" action="#{vpnUsersMBean.generateNewKeyPair}"/>--%>
	<%--</h:panelGrid>--%>
	<%--</h:form>--%>
	<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />
</body>
</f:view>
</html>
