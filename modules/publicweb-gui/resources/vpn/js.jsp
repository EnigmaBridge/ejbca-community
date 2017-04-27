<%@ page import="org.ejbca.core.ejb.vpn.*" %>
<%@ page import="org.ejbca.ui.web.pub.vpn.VpnLinkError" %>
<%@ page import="org.ejbca.core.ejb.vpn.useragent.OperatingSystem" %>
<%@ page contentType="application/json;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8"%>
<%
    response.setContentType("application/json; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding());
    response.setHeader("Content-Disposition", "inline");
    org.ejbca.ui.web.RequestHelper.setDefaultCharacterEncoding(request);
%>
<jsp:useBean id="vpnBean" class="org.ejbca.ui.web.pub.vpn.VpnBean" scope="session" />
<%
    vpnBean.initialize(request);
    vpnBean.isOtpValid();
    vpnBean.json(response);
%>