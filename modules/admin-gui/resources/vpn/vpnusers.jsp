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

<%@ page pageEncoding="UTF-8"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@ page errorPage="/errorpage.jsp" import="
org.ejbca.ui.web.admin.configuration.EjbcaWebBean,
org.ejbca.config.GlobalConfiguration,
org.ejbca.core.model.authorization.AccessRulesConstants,
org.cesecore.authorization.control.CryptoTokenRules
"%>
<%@ page import="org.cesecore.certificates.endentity.EndEntityConstants" %>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<%!
	static final String USER_PARAMETER           = "username";
	static final String HIDDEN_USERNAME          = "hiddenusername";
	static final String HIDDEN_RECORDNUMBER      = "hiddenrecordnumber";
	static final String SELECT_REVOKE_REASON     = "selectrevokereason";
%>

<%
	// TODO: fix resource for ACL
	GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, CryptoTokenRules.BASE.resource());
	final String VIEWCERT_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "viewcertificate.jsp";
	final String VIEWUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/viewendentity.jsp";
	final String EDITUSER_LINK            = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/editendentity.jsp";
	final String VIEWHISTORY_LINK         = ejbcawebbean.getBaseUrl() + globalconfiguration.getRaPath() + "/viewhistory.jsp";
	final String VIEWTOKEN_LINK           = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "hardtoken/viewtoken.jsp";

%>
<html>
<f:view>
<head>
  <title><h:outputText value="#{web.ejbcaWebBean.globalConfiguration.ejbcaTitle}" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
	<script src="<%= globalconfiguration.getAdminWebPath() %>jquery-1.12.4.min.js"></script>
	<script src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
  <script>
	/** Prevent form submission if enter is pressed in form and instead clicks on the button right of the inputText instead..) */
	function preventSubmitOnEnter(o, e) {
		if (typeof e == 'undefined' && window.event) {
			e = window.event;
		}
		if (e.keyCode == 13) {
			e.returnValue = false;
			o.nextSibling.click();
		}
	}

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

    function viewtoken(row){
        var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
        var username = hiddenusernamefield.value;
        var link = "<%= VIEWTOKEN_LINK %>?<%= USER_PARAMETER %>="+username;
        link = encodeURI(link);
        win_popup = window.open(link, 'view_token','height=650,width=750,scrollbars=yes,toolbar=no,resizable=1');
        win_popup.focus();
    }

    function confirmdelete(){
        var returnval;
        returnval = confirm("<%= ejbcawebbean.getText("AREYOUSUREDELETE",true) %>");
        returnval = returnval && confirm("<%= ejbcawebbean.getText("HAVEYOUREVOKEDTHEENDENTITIES",true) %>");

        return returnval;
    }

    function confirmdeleterevoke(){
        var returnval;
        returnval = confirm("<%= ejbcawebbean.getText("AREYOUSUREDELETEREVOKE",true) %>");

        return returnval;
    }

    function confirmrevocation(){
        var returnval = false;
        if(document.form.<%= SELECT_REVOKE_REASON %>.options.selectedIndex == -1){
            alert("<%= ejbcawebbean.getText("AREVOKEATIONREASON", true) %>");
            returnval = false;
        }else{
            returnval = confirm("<%= ejbcawebbean.getText("AREYOUSUREREVOKE",true) %>");
        }
        return returnval;
    }

    /**
	 * Select all
     */
    $(window).load(function () {
        $(document).delegate('.checkAll', 'click', function(event) {
            var isChecked = this.checked;
            $('.checkAnchor').prop('checked', isChecked);
            //$(this).closest("table").find(':checkbox').attr('checked', isChecked);
        });
    });
  </script>
   
</head>
<body>
	<h:outputText value="" rendered="#{vpnUsersMBean.pageLoadResetTrigger}"/>
	<h1>
		<h:outputText value="#{web.text.MANAGEVPNUSERS}"/>
		<%= ejbcawebbean.getHelpReference("/userguide.html#Managing%20Crypto%20Tokens") %>
	</h1>
	<div class="message"><h:messages layout="table" errorClass="alert"/></div>
	<h:form id="vpnusers">
	<h:dataTable value="#{vpnUsersMBean.vpnUserGuiList}" var="vpnUserGuiInfo" styleClass="grid">
		<%--<input type="hidden" name='<%= HIDDEN_USERNAME + i %>' value='<c:out value="<%= java.net.URLEncoder.encode(users[i].getEmail(),\"UTF-8\") %>"/>' >--%>
		<h:column>
			<f:facet name="header"><h:selectBooleanCheckbox styleClass="checkAll"/></f:facet>
			<h:selectBooleanCheckbox value="#{vpnUserGuiInfo.selected}" styleClass="checkAnchor"/>
		</h:column>

		<h:column>
   			<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_NAME}"/></f:facet>
			<h:outputLink value="adminweb/vpn/vpnuser.jsf?vpnUserId=#{vpnUserGuiInfo.id}&ref=default">
				<h:outputText value="#{vpnUserGuiInfo.userDesc}" title="#{web.text.VPNUSER_VIEWWITH} #{vpnUserGuiInfo.userDesc}"/>
			</h:outputLink>
		</h:column>

		<h:column>
   			<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_DATE_MODIFIED}"/></f:facet>
			<h:outputText id="dateModified" value="#{vpnUserGuiInfo.dateModified}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>
		</h:column>

		<h:column>
   			<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_MAIL_SENT}"/></f:facet>
			<h:outputText value="#{web.text.VPNUSER_DASH}" rendered="#{vpnUserGuiInfo.lastMailSent == null}"/>
			<h:outputText id="lastMailSent" value="#{vpnUserGuiInfo.lastMailSent}" rendered="#{vpnUserGuiInfo.lastMailSent != null}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>
		</h:column>

		<h:column>
   			<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_OTP_USED}"/></f:facet>
			<h:outputText id="otpUsed" value="#{vpnUserGuiInfo.otpUsed}" rendered="#{vpnUserGuiInfo.otpUsed != null}">
				<f:convertDateTime pattern="dd.MM.yyyy HH:mm:ss" />
			</h:outputText>
			<h:commandButton value="#{web.text.VPNUSER_SEND_EMAIL}" action="#{vpnUsersMBean.sendConfigEmail}"
							 rendered="#{vpnUserGuiInfo.otpDownload != null}" />
		</h:column>

		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_EMAIL}"/></f:facet>--%>
			<%--<h:outputText value="#{vpnUserGuiInfo.email}"/>--%>
		<%--</h:column>--%>

		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_DEVICE}"/></f:facet>--%>
			<%--<h:outputText value="#{vpnUserGuiInfo.device}"/>--%>
		<%--</h:column>--%>

		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_TYPE}"/></f:facet>--%>
			<%--<h:outputText value="#{web.text.CRYPTOTOKEN_TYPE_P11}" rendered="#{cryptoTokenGuiInfo.p11SlotType}"/>--%>
			<%--<h:outputText value="#{web.text.CRYPTOTOKEN_TYPE_SOFT}" rendered="#{!cryptoTokenGuiInfo.p11SlotType}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_ACTIVE}"/></f:facet>--%>
			<%--<h:graphicImage height="16" width="16" url="#{cryptoTokenGuiInfo.statusImg}" styleClass="statusIcon"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_AUTO}"/></f:facet>--%>
			<%--<h:graphicImage height="16" width="16" url="#{cryptoTokenGuiInfo.autoActivationYesImg}" styleClass="statusIcon" rendered="#{cryptoTokenGuiInfo.autoActivation}"/>--%>
		<%--</h:column>--%>
		<%--<h:column>--%>
   			<%--<f:facet name="header"><h:outputText value="#{web.text.CRYPTOTOKEN_REFDHEAD}"/></f:facet>--%>
			<%--<h:outputText value="#{web.text.CRYPTOTOKEN_UNUSED}" rendered="#{!cryptoTokenGuiInfo.referenced}"/>--%>
			<%--<h:outputText value="#{web.text.CRYPTOTOKEN_REFD}" rendered="#{cryptoTokenGuiInfo.referenced}"/>--%>
		<%--</h:column>--%>
		<h:column>
			<f:facet name="header"><h:outputText value="#{web.text.VPNUSER_STATUS}"/></f:facet>
			<h:outputText value="#{vpnUserGuiInfo.statusText}" rendered="#{vpnUserGuiInfo.statusText != null}"/>
		</h:column>

		<h:column>
   			<f:facet name="header">
			<h:panelGroup>
   				<h:outputText value="#{web.text.VPNUSER_ACTION}"/>
   				<%= ejbcawebbean.getHelpReference("/userguide.html#VPNUser%20action") %>
			</h:panelGroup>
   			</f:facet>

			<h:panelGroup rendered="#{vpnUserGuiInfo.userview != null}">
				<h:commandButton value="#{web.text.VPNUSER_VIEW_CERTIFICATE}"
								 onclick="return viewcert('#{vpnUserGuiInfo.userDesc}')"/>
				<h:commandButton value="#{web.text.VPNUSER_VIEW_USER}"
								 onclick="return viewuser('#{vpnUserGuiInfo.userDesc}')"/>
			</h:panelGroup>

		</h:column>
	</h:dataTable>
	<br/>

	<h:panelGroup>
		<h:commandButton value="#{web.text.VPNUSER_REFRESH}" action="#{vpnUsersMBean.refreshPage}"/>

		<h:commandButton value="#{web.text.VPNUSER_DELETE}" action="#{vpnUsersMBean.deleteVpnUsers}"
						 rendered="#{vpnUsersMBean.allowedToDelete}" onclick="return confirm('#{web.text.VPNUSER_CONF_DELETE}')"/>

		<h:commandButton value="#{web.text.VPNUSER_REVOKE}" action="#{vpnUsersMBean.revokeVpnUsers}"
						 rendered="#{vpnUsersMBean.allowedToDelete}" onclick="return confirm('#{web.text.VPNUSER_CONF_REVOKE}')"/>

		<h:commandButton value="#{web.text.VPNUSER_REGENERATE}" action="#{vpnUsersMBean.regenerateVpnUsers}"
						 rendered="#{vpnUsersMBean.allowedToDelete}" onclick="return confirm('#{web.text.VPNUSER_CONF_REGENERATE}')"/>
	</h:panelGroup>
	<br/>



	<h:outputLink value="adminweb/vpn/vpnuser.jsf?vpnUserId=&ref=vpnusers" rendered="#{cryptoTokenMBean.allowedToModify}">
		<h:outputText value="#{web.text.CRYPTOTOKEN_CREATENEW}"/>
	</h:outputLink>

	</h:form>
	<%	// Include Footer 
	String footurl = globalconfiguration.getFootBanner(); %>
	<jsp:include page="<%= footurl %>" />
</body>
</f:view>
</html>