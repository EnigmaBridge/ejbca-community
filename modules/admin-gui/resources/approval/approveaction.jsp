<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page pageEncoding="ISO-8859-1" errorPage="/errorpage.jsp" import="
org.ejbca.config.GlobalConfiguration,org.ejbca.ui.web.RequestHelper,
org.ejbca.ui.web.admin.configuration.EjbcaJSFHelper,
org.ejbca.core.model.authorization.AccessRulesConstants
"%>

<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:setProperty name="ejbcawebbean" property="*" /> 
<%   // Initialize environment
 GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR); 
 EjbcaJSFHelper helpbean = EjbcaJSFHelper.getBean();
 helpbean.setEjbcaWebBean(ejbcawebbean);
 helpbean.authorizedToApprovalPages();
%>
<html>
<head>
  <title><c:out value="<%= globalconfiguration.getEjbcaTitle() %>" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <meta http-equiv="Content-Type" content="text/html; charset=<%= org.ejbca.config.WebConfiguration.getWebContentEncoding() %>" />
</head>

<f:view>
<body onload='resize()'>

<script type="text/javascript">
<!--
function viewcert(link){
    enclink = encodeURI(link);
    win_popup = window.open(enclink, 'view_cert','height=650,width=600,scrollbars=yes,toolbar=no,resizable=1');
    win_popup.focus();
}
-->
</script>

<h2 align="center"><h:outputText value="#{web.text.APPROVEACTION}"/></h2>
<h:form>
  <h:inputHidden id="approveActionID" value="#{approvalActionRequest.uniqueId}"/>
  <f:attribute name="windowWidth" value="#{approvalActionManagedBean.windowWidth}"/>

   <h3 align="center">
     <h:outputText value="#{approvalActionManagedBean.approveRequestData.approveActionName}"/>
     <br /><h:messages  layout="table" errorClass="alert"/><br />
     <h:outputText value="#{web.text.CURRENTSTATUS}"/> <h:outputText value=" : "/> <h:outputText value="#{approvalActionManagedBean.approveRequestData.status}"/><br />     
   </h3>

   	<table border="0" cellpadding="5" width="100%">
   	   	<tr id="Row0">
   			<td><h:outputText value="#{web.text.REQUESTDATE}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.requestDate}"/></td>
   		</tr>
   	   	<tr id="Row1">
   			<td><h:outputText value="#{web.text.EXPIREDATE}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.expireDate}"/></td>
   		</tr>
   	   	<tr id="Row0">
   			<td><h:outputText value="#{web.text.REQUESTINGADMIN}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.requestAdminName}"/></td>
   		</tr>
   	   	<tr id="Row1">
   			<td><h:outputText value="#{web.text.RELATEDCA}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.caName}"/></td>
   		</tr>
   	   	<tr id="Row0">
   			<td><h:outputText value="#{web.text.RELATEDEEPROFILE}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.endEntityProfileName}"/></td>
   		</tr>
   		<tr id="Row1">
   			<td><h:outputText value="#{web.text.REMAININGAPPROVALS}"/></td>
   			<td><h:outputText value="#{approvalActionManagedBean.approveRequestData.remainingApprovals}"/></td>
   		</tr>
   	</table>

   

   <f:verbatim>
    <f:subview id="showcmp" rendered="#{approvalActionManagedBean.approvalRequestComparable}">
      <h:dataTable value="#{approvalActionManagedBean.approveRequestData.textComparisonList}" var="textCompareRow"  width="100%">
        <h:column>
          <f:facet name="header">
            <h:outputText value="#{web.text.ORIGINALACTIONDATA}"/>
          </f:facet>
          <h:outputText value="#{textCompareRow.orgvalue}"   styleClass="#{textCompareRow.textComparisonColor}"/>
        </h:column>
        <h:column>
          <f:facet name="header">
            <h:outputText value="#{web.text.REQUESTEDACTIONDATA}"/>
          </f:facet>
          <h:outputText value="#{textCompareRow.newvalue}" styleClass="#{textCompareRow.textComparisonColor}"/>
        </h:column>
      </h:dataTable>
     </f:subview>
     <f:subview id="shownoncmp" rendered="#{!approvalActionManagedBean.approvalRequestComparable and !approvalActionManagedBean.approveRequestData.containingLink}">
      <p align="center">
      <h:dataTable value="#{approvalActionManagedBean.approveRequestData.textComparisonList}" var="singleTextCompareRow"  width="100%">
        <h:column>
          <f:facet name="header">
            <h:outputText value="#{web.text.REQUESTEDACTIONDATA}"/>
          </f:facet>
          <h:outputText value="#{singleTextCompareRow.newvalue}"/>
        </h:column>
      </h:dataTable>
      </p>
     </f:subview>
     <f:subview id="shownoncmpwithlinks" rendered="#{!approvalActionManagedBean.approvalRequestComparable and approvalActionManagedBean.approveRequestData.containingLink}">
      <p align="center">
      <h:dataTable value="#{approvalActionManagedBean.approveRequestData.textListExceptLinks}" var="singleTextCompareRow"  width="100%">
        <h:column>
          <f:facet name="header">
            <h:outputText value="#{web.text.REQUESTEDACTIONDATA}"/>
          </f:facet>
          <h:outputText value="#{singleTextCompareRow.newvalue}"/>
        </h:column>
      </h:dataTable>
      <h:dataTable value="#{approvalActionManagedBean.approveRequestData.approvalDataLinks}" var="link"  width="100%">
        <h:column>
          <h:outputText value="#{link.preDescription}"/>
          <h:outputLink value="#{link.URI}" target="Viewinfo" onclick="#{approvalView.viewApproverCertLink}">
            <h:outputText value="#{link.description}"/>
          </h:outputLink>
          <h:outputText value="#{link.postDescription}"/>
        </h:column>
      </h:dataTable>
      </p>
     </f:subview>
  </f:verbatim>
<br/>
<br/>
    <h3 align="center"><h:outputText value="#{web.text.APPROVEDBY}"/></h3>
 
  <h:dataTable id="approvalTable" value="#{approvalActionManagedBean.approvalViews}" var="approvalView" width="100%">
    <h:column>
      <f:facet name="header">
        <h:panelGroup>
          <h:outputText value="#{web.text.ACTION}"/>
        </h:panelGroup>
      </f:facet>
      <h:outputText value="#{approvalView.adminAction}"/>
    </h:column>
    <h:column>
      <f:facet name="header">
        <h:outputText value="#{web.text.DATE}"/>
      </f:facet>
      <h:outputText value="#{approvalView.approvalDate}"/>
    </h:column>
    <h:column>
      <f:facet name="header">
        <h:outputText value="#{web.text.ADMINISTRATOR}"/>
      </f:facet>
          <h:outputLink value="" target="Viewinfo" onclick="#{approvalView.viewApproverCertLink}">
            <h:outputText  value="#{approvalView.approvalAdmin}"/>            
          </h:outputLink>         
    </h:column>
    <h:column>
      <f:facet name="header">
        <h:panelGroup>
          <h:outputText value="#{web.text.COMMENT}"/>
        </h:panelGroup>
      </f:facet>
      <h:outputText value="#{approvalView.comment}"/>
    </h:column>
  </h:dataTable>   
    <p align="center">
    <f:subview id="shownonerow" rendered="#{!approvalActionManagedBean.existsApprovals}">
      <h3 align="center"><h:outputText value="#{web.text.NONE}"/></h3>
    </f:subview>    
    <br /><br /><br />       
      <f:subview id="showapprovebuttons" rendered="#{approvalActionManagedBean.approvable}">
        <h:outputText value="#{web.text.COMMENT}"/><h:outputText value=":"/> 
        <h:inputTextarea id="comment" rows="2" cols="30" value="#{approvalActionManagedBean.comment}"/><br />
        <h:commandButton  id="accept" value="#{web.text.APPROVE}" action="#{approvalActionManagedBean.approve}" onclick="return confirmapprove()"/>
        <h:commandButton  id="reject" value="#{web.text.REJECT}" action="#{approvalActionManagedBean.reject}" onclick="return confirmreject()"/>
       </f:subview>
      <h:commandButton  id="button" value="#{web.text.CLOSE}" onclick="self.close()"/>    
    </p>
 </h:form>

  <script language="javascript">
<!--
function resize(){
  window.resizeTo(<h:outputText value="#{approvalActionManagedBean.windowWidth}"/>,800);
}
function confirmapprove(){
  return confirm('<h:outputText value="#{web.text.AREYOUSUREAPPROVE}"/>');
}
function confirmreject(){
  return confirm('<h:outputText value="#{web.text.AREYOUSUREREJECT}"/>'); 
}
-->
</script>
</body>
</f:view>
</html>

