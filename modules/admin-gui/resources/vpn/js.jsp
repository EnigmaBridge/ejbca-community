<%@ page import="org.ejbca.core.ejb.vpn.*"
%><%@ page import="org.ejbca.config.GlobalConfiguration"
%><%@ page import="org.ejbca.core.model.authorization.AccessRulesConstants"
%><%@ page import="org.cesecore.authorization.control.CryptoTokenRules"
%><%@ page contentType="application/json;charset=UTF-8" language="java"
%><%@ page pageEncoding="UTF-8"
%><%
    response.setContentType("application/json; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding());
    response.setHeader("Content-Disposition", "inline");
    org.ejbca.ui.web.RequestHelper.setDefaultCharacterEncoding(request);
%>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="vpnUsersMBean" scope="session" class="org.ejbca.ui.web.admin.vpn.VpnUsersMBean" />
<%
    GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, CryptoTokenRules.BASE.resource());
    vpnUsersMBean.json(request, response);
%>