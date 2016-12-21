<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page pageEncoding="ISO-8859-1"%>
<% response.setContentType("text/html; charset="+org.ejbca.config.WebConfiguration.getWebContentEncoding()); %>
<%@page errorPage="/errorpage.jsp" import="
java.util.*,
java.util.Map.Entry,
java.io.*,
java.security.cert.Certificate,
org.apache.commons.fileupload.*,
org.ejbca.ui.web.admin.configuration.EjbcaWebBean,
org.ejbca.config.GlobalConfiguration,
org.cesecore.authorization.control.StandardRules,
org.cesecore.util.FileTools,
org.cesecore.util.CertTools,
org.cesecore.CesecoreException,
org.cesecore.authorization.AuthorizationDeniedException,
org.ejbca.ui.web.RequestHelper,
org.ejbca.ui.web.admin.cainterface.CAInterfaceBean,
org.cesecore.certificates.ca.CAInfo,
org.cesecore.certificates.ca.X509CAInfo,
org.cesecore.certificates.ca.CVCCAInfo,
org.cesecore.certificates.ca.catoken.CAToken,
org.cesecore.certificates.ca.CAConstants,
org.ejbca.core.model.SecConst,
org.cesecore.certificates.ca.catoken.CATokenConstants,
org.cesecore.certificates.ca.catoken.CAToken,
org.ejbca.ui.web.admin.cainterface.CADataHandler,
org.ejbca.ui.web.RevokedInfoView,
org.ejbca.ui.web.admin.configuration.InformationMemory,
org.bouncycastle.asn1.x500.X500Name,
org.ejbca.core.EjbcaException,
org.cesecore.certificates.certificate.request.PKCS10RequestMessage,
org.cesecore.certificates.certificate.request.RequestMessage,
org.cesecore.certificates.certificate.request.RequestMessageUtils,
org.cesecore.certificates.certificate.request.CVCRequestMessage,
org.cesecore.certificates.ca.CAExistsException,
org.cesecore.certificates.ca.CADoesntExistsException,
org.cesecore.keys.token.CryptoTokenOfflineException,
org.cesecore.keys.token.CryptoTokenAuthenticationFailedException,
org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAServiceInfo,
org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo,
org.ejbca.core.model.ca.caadmin.extendedcaservices.HardTokenEncryptCAServiceInfo,
org.ejbca.core.model.ca.caadmin.extendedcaservices.KeyRecoveryCAServiceInfo,
org.cesecore.certificates.ca.extendedservices.ExtendedCAServiceInfo,
org.cesecore.keys.token.AvailableCryptoToken,
org.cesecore.certificates.ca.catoken.CATokenConstants,
org.cesecore.certificates.util.DNFieldExtractor,
org.cesecore.certificates.util.DnComponents,
org.cesecore.keys.token.CryptoToken,
org.cesecore.keys.token.BaseCryptoToken,
org.cesecore.keys.token.NullCryptoToken,
org.cesecore.keys.token.SoftCryptoToken,
org.cesecore.keys.token.PKCS11CryptoToken,
org.cesecore.certificates.certificateprofile.CertificateProfile,
org.cesecore.certificates.certificateprofile.CertificatePolicy,
org.ejbca.ui.web.admin.cainterface.CAInfoView,
org.bouncycastle.jce.exception.ExtCertPathValidatorException,
org.cesecore.util.SimpleTime,
org.cesecore.util.YearMonthDayTime,
org.ejbca.util.CombineTime,
org.cesecore.util.ValidityDate,
org.ejbca.ui.web.ParameterException,
org.cesecore.util.StringTools,
org.cesecore.certificates.util.AlgorithmConstants,
org.cesecore.certificates.util.AlgorithmTools,
org.cesecore.certificates.certificate.certextensions.standard.NameConstraint,
org.ejbca.core.model.authorization.AccessRulesConstants,
java.security.cert.CertificateException,
javax.ejb.EJBException,
java.security.InvalidParameterException,
java.security.InvalidAlgorithmParameterException
" %>
<html>
<jsp:useBean id="ejbcawebbean" scope="session" class="org.ejbca.ui.web.admin.configuration.EjbcaWebBean" />
<jsp:useBean id="cabean" scope="session" class="org.ejbca.ui.web.admin.cainterface.CAInterfaceBean" />

<%! // Declarations 
  static final String ACTION                              = "action";
  static final String ACTION_EDIT_CAS                     = "editcas";
  static final String ACTION_EDIT_CA                      = "editca";
  static final String ACTION_CREATE_CA                    = "createca";
  static final String ACTION_CHOOSE_CATYPE                = "choosecatype";
  static final String ACTION_CHOOSE_CATOKENTYPE           = "choosecatokentype";
  static final String ACTION_CHOOSE_CASIGNALGO            = "choosecasignalgo";
  static final String ACTION_SIGNREQUEST                  = "signrequest";
  static final String ACTION_IMPORTCA		              = "importca";
  static final String ACTION_IMPORTCACERT	              = "importcacert";

  static final String CHECKBOX_VALUE           = "true";

  //  Used in choosecapage.jsp
  static final String BUTTON_EDIT_CA                       = "buttoneditca"; 
  static final String BUTTON_DELETE_CA                     = "buttondeleteca";
  static final String BUTTON_CREATE_CA                     = "buttoncreateca"; 
  static final String BUTTON_RENAME_CA                     = "buttonrenameca";
  static final String BUTTON_SIGNREQUEST                   = "buttonsignrequest";
  static final String BUTTON_IMPORTCA		               = "buttonimportca";
  static final String BUTTON_EXPORTCA		               = "buttonexportca";
  static final String BUTTON_IMPORTCACERT	               = "buttonimportcacert";

  static final String SELECT_CAS                           = "selectcas";
  static final String TEXTFIELD_CANAME                     = "textfieldcaname";
  static final String HIDDEN_CANAME                        = "hiddencaname";
  static final String HIDDEN_CAID                          = "hiddencaid";
  static final String HIDDEN_CATYPE                        = "hiddencatype";
  static final String HIDDEN_CASIGNALGO                    = "hiddencasignalgo";
  static final String HIDDEN_CACRYPTOTOKEN                 = "hiddencacryptotoken";
  static final String HIDDEN_KEYSIZE                       = "hiddenkeysize";
  static final String HIDDEN_RENEWKEYS                     = "hiddenrenewkeys";
 
  // Buttons used in editcapage.jsp
  static final String BUTTON_SAVE                       = "buttonsave";
  static final String BUTTON_SAVE_EXTERNALCA            = "buttonsaveexternalca";
  static final String BUTTON_CREATE                     = "buttoncreate";
  static final String BUTTON_CANCEL                     = "buttoncancel";
  static final String BUTTON_INITIALIZE                 = "buttoninitialize";
  static final String BUTTON_MAKEREQUEST                = "buttonmakerequest";
  static final String BUTTON_RECEIVEREQUEST             = "buttonreceiverequest";
  static final String BUTTON_RENEWCA                    = "buttonrenewca";
  static final String BUTTON_REVOKECA                   = "buttonrevokeca";  
  static final String BUTTON_RECIEVEFILE                = "buttonrecievefile";     
  static final String BUTTON_PUBLISHCA                  = "buttonpublishca";     
  static final String BUTTON_REVOKERENEWXKMSCERTIFICATE = "checkboxrenewxkmscertificate";
  static final String BUTTON_REVOKERENEWCMSCERTIFICATE  = "checkboxrenewcmscertificate";
  static final String BUTTON_GENDEFAULTCRLDISTPOINT     = "checkboxgeneratedefaultcrldistpoint";
  static final String BUTTON_GENDEFAULTCRLISSUER        = "checkboxgeneratedefaultcrlissuer";
  static final String BUTTON_GENCADEFINEDFRESHESTCRL    = "checkboxgeneratecadefinedfresherstcrl";
  static final String BUTTON_GENDEFAULTOCSPLOCATOR      = "checkbexgeneratedefaultocsplocator";
  static final String BUTTON_RECEIVE_IMPORT_RENEWAL     = "buttonreceiveimportrenewal";

  static final String TEXTFIELD_KEYSEQUENCE           = "textfieldkeysequence";
  static final String TEXTFIELD_SUBJECTDN             = "textfieldsubjectdn";
  static final String TEXTFIELD_SUBJECTALTNAME        = "textfieldsubjectaltname";
  static final String TEXTFIELD_EXTERNALCDP           = "textfieldexternalcdp";
  static final String TEXTFIELD_CRLPERIOD             = "textfieldcrlperiod";
  static final String TEXTFIELD_CRLISSUEINTERVAL      = "textfieldcrlissueinterval";
  static final String TEXTFIELD_CRLOVERLAPTIME        = "textfieldcrloverlaptime";
  static final String TEXTFIELD_DELTACRLPERIOD        = "textfielddeltacrlperiod";
  static final String TEXTFIELD_DESCRIPTION           = "textfielddescription";
  static final String TEXTFIELD_VALIDITY              = "textfieldvalidity";
  static final String TEXTFIELD_POLICYID              = "textfieldpolicyid";
  static final String TEXTFIELD_DEFAULTCRLDISTPOINT   = "textfielddefaultcrldistpoint";
  static final String TEXTFIELD_DEFAULTCRLISSUER      = "textfielddefaultcrlissuer";
  static final String TEXTFIELD_DEFAULTOCSPLOCATOR    = "textfielddefaultocsplocator";
  static final String TEXTFIELD_CADEFINEDFRESHESTCRL  = "textfieldcadefinedfreshestcrl";
  static final String TEXTFIELD_IMPORTCA_PASSWORD	  = "textfieldimportcapassword";
  static final String TEXTFIELD_IMPORTCA_SIGKEYALIAS  = "textfieldimportcasigkeyalias";
  static final String TEXTFIELD_IMPORTCA_ENCKEYALIAS  = "textfieldimportcaenckeyalias";
  static final String TEXTFIELD_IMPORTCA_NAME		  = "textfieldimportcaname";
  static final String TEXTFIELD_SHAREDCMPRASECRET     = "textfieldsharedcmprasecret";
  static final String TEXTFIELD_AUTHORITYINFORMATIONACCESS  = "textfieldauthorityinformationaccess";
  static final String TEXTFIELD_NAMECONSTRAINTSPERMITTED    = "textfieldnameconstraintspermitted";
  static final String TEXTFIELD_NAMECONSTRAINTSEXCLUDED     = "textfieldnameconstraintsexcluded";


  static final String CHECKBOX_AUTHORITYKEYIDENTIFIER             = "checkboxauthoritykeyidentifier";
  static final String CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL     = "checkboxauthoritykeyidentifiercritical";
  static final String CHECKBOX_USECRLNUMBER                       = "checkboxusecrlnumber";
  static final String CHECKBOX_CRLNUMBERCRITICAL                  = "checkboxcrlnumbercritical";
  static final String CHECKBOX_FINISHUSER                         = "checkboxfinishuser";
  static final String CHECKBOX_DOENFORCEUNIQUEPUBLICKEYS          = "isdoenforceuniquepublickeys";
  static final String CHECKBOX_DOENFORCEUNIQUEDN                  = "isdoenforceuniquedn";
  static final String CHECKBOX_DOENFORCEUNIQUESUBJECTDNSERIALNUMBER="doenforceuniquesubjectdnerialnumber";
  static final String CHECKBOX_USECERTREQHISTORY                  = "checkboxusecertreqhistory";
  static final String CHECKBOX_USEUSERSTORAGE                     = "checkboxuseuserstorage";
  static final String CHECKBOX_USECERTIFICATESTORAGE              = "checkboxusecertificatestorage";
  static final String CHECKBOX_USEUTF8POLICYTEXT                  = "checkboxuseutf8policytext";
  static final String CHECKBOX_USEPRINTABLESTRINGSUBJECTDN        = "checkboxuseprintablestringsubjectdn";
  static final String CHECKBOX_USELDAPDNORDER                     = "checkboxuseldapdnorder";
  static final String CHECKBOX_USECRLDISTRIBUTIONPOINTONCRL       = "checkboxusecrldistributionpointoncrl";
  static final String CHECKBOX_CRLDISTRIBUTIONPOINTONCRLCRITICAL  = "checkboxcrldistributionpointoncrlcritical";
  static final String CHECKBOX_CREATELINKCERTIFICATE              = "checkboxcreatelinkcertificate";
  
  static final String CHECKBOX_ACTIVATEOCSPSERVICE                = "checkboxactivateocspservice";  
  static final String CHECKBOX_ACTIVATEXKMSSERVICE                = "checkboxactivatexkmsservice";
  static final String CHECKBOX_ACTIVATECMSSERVICE                 = "checkboxactivatecmsservice";
  
  static final String SELECT_REVOKEREASONS                        = "selectrevokereasons";
  static final String SELECT_CATYPE                               = "selectcatype";  
  static final String SELECT_CRYPTOTOKEN                          = "selectcryptotoken";
  static final String SELECT_CRYPTOTOKEN_DEFAULTKEY               = "selectdefaultkey";
  static final String SELECT_CRYPTOTOKEN_CERTSIGNKEY              = "selectcertsignkey";
  //static final String SELECT_CRYPTOTOKEN_CRLSIGNKEY               = "selectcrlsignkey";
  static final String SELECT_CRYPTOTOKEN_KEYENCRYPTKEY            = "selectkeyencryptkey";
  static final String SELECT_CRYPTOTOKEN_KEYTESTKEY               = "selectkeytestkey";
  static final String SELECT_CRYPTOTOKEN_HARDTOKENENCRYPTKEY      = "selecthardtokenencryptkey";
  static final String SELECT_CRYPTOTOKEN_CERTSIGNKEY_RENEW        = "selectcertsignkeyrenew";
  static final String SELECT_CRYPTOTOKEN_CERTSIGNKEY_MAKEREQUEST  = "selectcertsignkeymakerequest";
  static final String SELECT_CRYPTOTOKEN_CERTSIGNKEY_RECEIVEREQ   = "selectcertsignkeyreceivereq";
  static final String SELECT_SIGNEDBY                             = "selectsignedby"; 
  static final String SELECT_KEYSIZE                              = "selectsize";
  static final String SELECT_KEY_SEQUENCE_FORMAT                  = "selectkeysequenceformat";
  static final String SELECT_AVAILABLECRLPUBLISHERS               = "selectavailablecrlpublishers";
  static final String SELECT_CERTIFICATEPROFILE                   = "selectcertificateprofile";
  static final String SELECT_SIGNATUREALGORITHM                   = "selectsignaturealgorithm";
  static final String SELECT_APPROVALSETTINGS                     = "approvalsettings";
  static final String SELECT_NUMOFREQUIREDAPPROVALS               = "numofrequiredapprovals";

  static final String HIDDEN_INCLUDEINHEALTHCHECK                 = "includeinhealthcheck";
  
  static final String FILE_RECIEVEFILE                            = "filerecievefile";
  static final String FILE_RECIEVEFILE_MAKEREQUEST                = "filerecievefilemakerequest";
  static final String FILE_RECIEVEFILE_RECEIVEREQUEST             = "filerecievefilerecieverequest";
  static final String FILE_RECIEVEFILE_IMPORTED_RENEWAL           = "filerecievefileimportrenewal";

  static final String CERTSERNO_PARAMETER       = "certsernoparameter"; 

  static final int    CERTREQGENMODE      = 0;
  static final int    CERTGENMODE         = 1;
%><%       
  // Initialize environment
  String includefile = "choosecapage.jspf"; 
  int catype = CAInfo.CATYPE_X509;
  int keySequenceFormat = StringTools.KEY_SEQUENCE_FORMAT_NUMERIC;
  String cryptoTokenIdParam = "";
  String signatureAlgorithmParam = "";
  String extendedServicesKeySpecParam = null;

  boolean  caexists             = false;
  boolean  cadeletefailed       = false;
  boolean  illegaldnoraltname   = false;
  boolean  errorrecievingfile   = false;
  boolean  xkmsrenewed          = false;
  boolean  cmsrenewed           = false;
  boolean  catokenoffline       = false;
  boolean  initcatokenoffline   = false;
  boolean  catokenauthfailed    = false;
  String errormessage = null;

  GlobalConfiguration globalconfiguration = ejbcawebbean.initialize(request, AccessRulesConstants.ROLE_ADMINISTRATOR, StandardRules.CAFUNCTIONALITY.resource());
  cabean.initialize(ejbcawebbean);
  CADataHandler cadatahandler = cabean.getCADataHandler();

  final String THIS_FILENAME = globalconfiguration.getCaPath()  + "/editcas/editcas.jsp";
  final String VIEWCERT_LINK = ejbcawebbean.getBaseUrl() + globalconfiguration.getAdminWebPath() + "viewcertificate.jsp";

  boolean issuperadministrator = false;
  boolean editca = false;
  boolean caactivated = false;
  boolean carenewed = false;
  boolean capublished = false;

  int filemode = 0;
  int row = 0;

  Map<Integer,String> caidtonamemap = cabean.getCAIdToNameMap();
  InformationMemory info = ejbcawebbean.getInformationMemory();

%>
<head>
  <title><c:out value="<%= globalconfiguration.getEjbcaTitle() %>" /></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>" />
  <link rel="stylesheet" type="text/css" href="<%= ejbcawebbean.getCssFile() %>" />
  <script type="text/javascript" src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
</head>
<%
    RequestHelper.setDefaultCharacterEncoding(request);
    // Map both multipart requests parameters and regular requests parameteres to a single map
    final Map<String, String> requestMap = new HashMap<String, String>();
    final byte[] fileBuffer = cabean.parseRequestParameters(request, requestMap);
    // Parse request parameters
    final String action = requestMap.get(ACTION);
    int caid = 0;
    try {
        caid = Integer.parseInt(requestMap.get(HIDDEN_CAID));
    } catch (NumberFormatException e) {
    	final String selectCaString = requestMap.get(SELECT_CAS);
        try {
            caid = Integer.parseInt(selectCaString);
        } catch (Exception exception) { }
    }
    String caname = requestMap.get(HIDDEN_CANAME);
    boolean reGenerateKeys = Boolean.valueOf(requestMap.get(HIDDEN_RENEWKEYS)).booleanValue();
    boolean buttoncancel = requestMap.get(BUTTON_CANCEL) != null;
    String importcaname = requestMap.get(TEXTFIELD_IMPORTCA_NAME);
    String importpassword = requestMap.get(TEXTFIELD_IMPORTCA_PASSWORD);
    String importsigalias = requestMap.get(TEXTFIELD_IMPORTCA_SIGKEYALIAS);
    String importencalias = requestMap.get(TEXTFIELD_IMPORTCA_ENCKEYALIAS);

    try {
        if (ACTION_EDIT_CAS.equals(action)) {
        	final String textFieldCaName = requestMap.get(TEXTFIELD_CANAME);
        	if (textFieldCaName!=null) {
        		requestMap.put(TEXTFIELD_CANAME, textFieldCaName.trim());
        	}
            if (caid != 0) {
                if (requestMap.get(BUTTON_EDIT_CA) != null) {
                    editca = true;
                    catype = cadatahandler.getCAInfo(caid).getCAInfo().getCAType();
                    keySequenceFormat = cadatahandler.getCAInfo(caid).getCAToken().getKeySequenceFormat();
                    includefile = "editcapage.jspf";
                }
                if( requestMap.get(BUTTON_DELETE_CA) != null) {
                    // Delete profile and display choosecapage. 
                    cadeletefailed = !cadatahandler.removeCA(caid);
                }
                if (requestMap.get(BUTTON_RENAME_CA) != null) {
                    // Rename selected CA and display choosecapage.
        	        caexists = cadatahandler.renameCA(caid, requestMap.get(TEXTFIELD_CANAME));
                }
                if (requestMap.get(BUTTON_SIGNREQUEST) != null) {
                    caname = (String) caidtonamemap.get(caid);
                    if (cabean.getCAInfo(caid) != null) {
                        //filemode = SIGNREQUESTMODE;
                        includefile="recievefile.jspf";            	  
                    }
                }
            }
            if (requestMap.get(BUTTON_IMPORTCA) != null) {
                // Import CA from p12-file. Start by prompting for file and keystore password.
		        includefile="importca.jspf";
		    }
            if (requestMap.get(BUTTON_IMPORTCACERT) != null) {
                // Import CA from p12-file. Start by prompting for file and keystore password.
		        includefile="importcacert.jspf";
		    }
            if (requestMap.get(BUTTON_CREATE_CA) != null) {
                // Add profile and display profilespage.
                includefile="choosecapage.jspf"; 
                caname = requestMap.get(TEXTFIELD_CANAME);
                if (caname != null && caname.length()>0) {
                    editca = false;
                    includefile="editcapage.jspf";              
                }
            }
        }
        if (ACTION_CREATE_CA.equals(action)) {
            boolean buttonCreateCa = requestMap.get(BUTTON_CREATE) != null;
            boolean buttonMakeRequest = requestMap.get(BUTTON_MAKEREQUEST) != null;
            if (buttonCreateCa || buttonMakeRequest) {
                // Create and save CA                          
                caname = requestMap.get(HIDDEN_CANAME);
                signatureAlgorithmParam = requestMap.get(HIDDEN_CASIGNALGO);
                final String signkeyspec = requestMap.get(SELECT_KEYSIZE);
                final String keySequenceFormatParam = requestMap.get(SELECT_KEY_SEQUENCE_FORMAT);
                final String keySequence = requestMap.get(TEXTFIELD_KEYSEQUENCE);
                catype = Integer.parseInt(requestMap.get(HIDDEN_CATYPE));
                final String subjectdn = requestMap.get(TEXTFIELD_SUBJECTDN);
                final String certificateProfileIdString = requestMap.get(SELECT_CERTIFICATEPROFILE);
                final String signedByString = requestMap.get(SELECT_SIGNEDBY);
                final String description = requestMap.get(TEXTFIELD_DESCRIPTION);
                final String validityString = requestMap.get(TEXTFIELD_VALIDITY);
                final String approvalSettingValues = requestMap.get(SELECT_APPROVALSETTINGS);//request.getParameterValues(SELECT_APPROVALSETTINGS);
                final String numofReqApprovalsParam = requestMap.get(SELECT_NUMOFREQUIREDAPPROVALS);
                final boolean finishUser = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_FINISHUSER));
                final boolean isDoEnforceUniquePublicKeys = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUEPUBLICKEYS));
                final boolean isDoEnforceUniqueDistinguishedName = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUEDN));
                final boolean isDoEnforceUniqueSubjectDNSerialnumber = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUESUBJECTDNSERIALNUMBER));
                final boolean useCertReqHistory = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECERTREQHISTORY));
                final boolean useUserStorage = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEUSERSTORAGE));
                final boolean useCertificateStorage = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECERTIFICATESTORAGE));
                final String subjectaltname = requestMap.get(TEXTFIELD_SUBJECTALTNAME);
                final String policyid = requestMap.get(TEXTFIELD_POLICYID);
                final boolean useauthoritykeyidentifier = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_AUTHORITYKEYIDENTIFIER));
                final boolean authoritykeyidentifiercritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL));
                // CRL periods and publishers is specific for X509 CAs
                final long crlperiod = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLPERIOD), "1"+CombineTime.TYPE_DAYS).getLong();
                final long crlIssueInterval = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLISSUEINTERVAL), "0"+CombineTime.TYPE_MINUTES).getLong();
                final long crlOverlapTime = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLOVERLAPTIME), "10"+CombineTime.TYPE_MINUTES).getLong();
                final long deltacrlperiod = CombineTime.getInstance(requestMap.get(TEXTFIELD_DELTACRLPERIOD), "0"+CombineTime.TYPE_MINUTES).getLong();              
                final String availablePublisherValues = requestMap.get(SELECT_AVAILABLECRLPUBLISHERS);//request.getParameterValues(SELECT_AVAILABLECRLPUBLISHERS);
                final boolean usecrlnumber = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECRLNUMBER));
                final boolean crlnumbercritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_CRLNUMBERCRITICAL));
                final String defaultcrldistpoint = requestMap.get(TEXTFIELD_DEFAULTCRLDISTPOINT);
                final String defaultcrlissuer = requestMap.get(TEXTFIELD_DEFAULTCRLISSUER);
                final String defaultocsplocator  = requestMap.get(TEXTFIELD_DEFAULTOCSPLOCATOR);
                final String authorityInformationAccess = requestMap.get(TEXTFIELD_AUTHORITYINFORMATIONACCESS);
                final String nameConstraintsPermitted = requestMap.get(TEXTFIELD_NAMECONSTRAINTSPERMITTED);
                final String nameConstraintsExcluded = requestMap.get(TEXTFIELD_NAMECONSTRAINTSEXCLUDED);
                final String caDefinedFreshestCrl = requestMap.get(TEXTFIELD_CADEFINEDFRESHESTCRL);
                final boolean useutf8policytext = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEUTF8POLICYTEXT));
                final boolean useprintablestringsubjectdn = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEPRINTABLESTRINGSUBJECTDN));
                final boolean useldapdnorder = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USELDAPDNORDER));
                final boolean usecrldistpointoncrl = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECRLDISTRIBUTIONPOINTONCRL));
                final boolean crldistpointoncrlcritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_CRLDISTRIBUTIONPOINTONCRLCRITICAL));
                final boolean serviceOcspActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATEOCSPSERVICE));
                final boolean serviceXkmsActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATEXKMSSERVICE));
                final boolean serviceCmsActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATECMSSERVICE));
                final String sharedCmpRaSecret = requestMap.get(TEXTFIELD_SHAREDCMPRASECRET);
                final String cryptoTokenIdString = requestMap.get(HIDDEN_CACRYPTOTOKEN); //requestMap.get(SELECT_CRYPTOTOKEN);
                final String keyAliasCertSignKey = requestMap.get(SELECT_CRYPTOTOKEN_CERTSIGNKEY);
                final String keyAliasCrlSignKey = keyAliasCertSignKey;//requestMap.get(SELECT_CRYPTOTOKEN_CRLSIGNKEY);
                final String keyAliasDefaultKey = requestMap.get(SELECT_CRYPTOTOKEN_DEFAULTKEY);
                final String keyAliasHardTokenEncryptKey = requestMap.get(SELECT_CRYPTOTOKEN_HARDTOKENENCRYPTKEY);
                final String keyAliasKeyEncryptKey = requestMap.get(SELECT_CRYPTOTOKEN_KEYENCRYPTKEY);
                final String keyAliasKeyTestKey = requestMap.get(SELECT_CRYPTOTOKEN_KEYTESTKEY);
                try {
                    illegaldnoraltname = cabean.actionCreateCaMakeRequest(caname, signatureAlgorithmParam,
                     signkeyspec, keySequenceFormatParam, keySequence,
               		 catype, subjectdn, certificateProfileIdString, signedByString, description, validityString,
               		 approvalSettingValues, numofReqApprovalsParam, finishUser, isDoEnforceUniquePublicKeys,
               		 isDoEnforceUniqueDistinguishedName,
               		 isDoEnforceUniqueSubjectDNSerialnumber, useCertReqHistory, useUserStorage, useCertificateStorage,
               		 subjectaltname, policyid, useauthoritykeyidentifier, authoritykeyidentifiercritical,
               		 crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, availablePublisherValues,
               		 usecrlnumber, crlnumbercritical, defaultcrldistpoint, defaultcrlissuer, defaultocsplocator,
               		 authorityInformationAccess, nameConstraintsPermitted, nameConstraintsExcluded,
               		 caDefinedFreshestCrl, useutf8policytext, useprintablestringsubjectdn, useldapdnorder,
               		 usecrldistpointoncrl, crldistpointoncrlcritical, serviceOcspActive, serviceXkmsActive,
               		 serviceCmsActive, sharedCmpRaSecret, buttonCreateCa, buttonMakeRequest,
               		 cryptoTokenIdString, keyAliasCertSignKey, keyAliasCrlSignKey, keyAliasDefaultKey,
               		 keyAliasHardTokenEncryptKey, keyAliasKeyEncryptKey, keyAliasKeyTestKey,
               		 fileBuffer);
                } catch (CAExistsException caee) {
                    caexists = true; 
                } catch (CryptoTokenAuthenticationFailedException catfe) {
                    catokenauthfailed = true;
                    errormessage = catfe.getMessage();
                    Throwable t = catfe.getCause();
                    while (t != null) {
                        String msg = t.getMessage();
                        if (msg != null) {
                            errormessage = errormessage + "<br/>" + msg;                            
                        }
                        t = t.getCause();
                    }
                } catch (ParameterException pe) {
                    errormessage = pe.getMessage();
                } catch (EJBException ejbe) {
                    Exception ex = ejbe.getCausedByException();
                    if (ex instanceof InvalidAlgorithmParameterException) {
                        errormessage = ejbcawebbean.getText("INVALIDSIGORKEYALGPARAM") + ": " + ex.getLocalizedMessage();
                    } else {
                        throw ejbe;
                    }
                }
                if (catype == CAInfo.CATYPE_X509 && crlperiod != 0 && !illegaldnoraltname && buttonCreateCa) {
                    includefile="choosecapage.jspf";
                }
                if (catype == CAInfo.CATYPE_CVC && !illegaldnoraltname && buttonCreateCa) {
                    caid = CertTools.stringToBCDNString(subjectdn).hashCode();
                    includefile="choosecapage.jspf";
                }
                if (buttonMakeRequest) {
                    filemode = CERTREQGENMODE;
                    includefile = "displayresult.jspf";
                }
            }
            if (requestMap.get(BUTTON_CANCEL) != null) {
                // Don't save changes.
                includefile="choosecapage.jspf"; 
            }
        }

        if (ACTION_EDIT_CA.equals(action)) {
            if (requestMap.get(BUTTON_REVOKERENEWCMSCERTIFICATE) != null) {
                cadatahandler.renewAndRevokeCmsCertificate(caid);
                cmsrenewed = true;             
            }
            if (requestMap.get(BUTTON_REVOKERENEWXKMSCERTIFICATE) != null) {
                cadatahandler.renewAndRevokeXKMSCertificate(caid);
                xkmsrenewed = true;             
            }
            if (requestMap.get(BUTTON_REVOKECA) != null) {
                final String revocationReasonParam = requestMap.get(SELECT_REVOKEREASONS);
                final int revokereason = (revocationReasonParam==null ? 0 : Integer.parseInt(revocationReasonParam));
                cadatahandler.revokeCA(caid, revokereason);                   
            }
            if (requestMap.get(BUTTON_RENEWCA) != null) {
                final String nextSignKeyAlias = requestMap.get(SELECT_CRYPTOTOKEN_CERTSIGNKEY_RENEW);
                final boolean createLinkCertificate = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_CREATELINKCERTIFICATE));
                try {
                    carenewed = cadatahandler.renewCA(caid, nextSignKeyAlias, createLinkCertificate);
                } catch (EjbcaException e) { 
                    includefile="choosecapage.jspf"; 
                    errormessage = e.getMessage(); 
                }
            }
            if (requestMap.get(BUTTON_RECEIVEREQUEST) != null) {
            	try {
                    final String nextSignKeyAlias = requestMap.get(SELECT_CRYPTOTOKEN_CERTSIGNKEY_RECEIVEREQ);
                    cadatahandler.receiveResponse(caid, fileBuffer, nextSignKeyAlias);
                    caactivated = true;
            	} catch (Exception e) {
            		errormessage = e.getMessage();
            	}
            }
            if (requestMap.get(BUTTON_RECEIVE_IMPORT_RENEWAL) != null) {
            	try {
                    cadatahandler.importCACertUpdate(caid, fileBuffer);
                    carenewed = true;
            	} catch (Exception e) {
            		errormessage = e.getMessage();
            	}
            }
            if (requestMap.get(BUTTON_PUBLISHCA) != null) {
                cadatahandler.publishCA(caid);
                capublished = true;             
            }
            if (requestMap.get(BUTTON_SAVE_EXTERNALCA) != null) {
            	if (cadatahandler.getCAInfo(caid).getCAInfo().getCAType()==CAInfo.CATYPE_X509) {
                	final String externalCdp = requestMap.get(TEXTFIELD_EXTERNALCDP).trim();
                	X509CAInfo x509caInfo = (X509CAInfo)cadatahandler.getCAInfo(caid).getCAInfo();
                	x509caInfo.setExternalCdp(externalCdp);
                	cadatahandler.editCA(x509caInfo);
            	}
            }
            if (requestMap.get(BUTTON_SAVE)  != null || requestMap.get(BUTTON_MAKEREQUEST) != null ||
                requestMap.get(BUTTON_INITIALIZE) != null) {
                // Create and save CA
                caname = requestMap.get(HIDDEN_CANAME);
                catype = Integer.parseInt(requestMap.get(HIDDEN_CATYPE));
                final String keySequenceFormatParam = requestMap.get(SELECT_KEY_SEQUENCE_FORMAT);
                final String keySequence = requestMap.get(TEXTFIELD_KEYSEQUENCE);
                final String description = requestMap.get(TEXTFIELD_DESCRIPTION);
                final String validityString = requestMap.get(TEXTFIELD_VALIDITY);
                final long crlperiod = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLPERIOD), "0"+CombineTime.TYPE_MINUTES).getLong();
                final long crlIssueInterval = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLISSUEINTERVAL), "0"+CombineTime.TYPE_MINUTES).getLong();
                final long crlOverlapTime = CombineTime.getInstance(requestMap.get(TEXTFIELD_CRLOVERLAPTIME), "0"+CombineTime.TYPE_MINUTES).getLong();
                final long deltacrlperiod = CombineTime.getInstance(requestMap.get(TEXTFIELD_DELTACRLPERIOD), "0"+CombineTime.TYPE_MINUTES).getLong();              
                final boolean finishUser = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_FINISHUSER));
                final boolean isDoEnforceUniquePublicKeys = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUEPUBLICKEYS));
                final boolean isDoEnforceUniqueDistinguishedName = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUEDN));
                final boolean isDoEnforceUniqueSubjectDNSerialnumber = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_DOENFORCEUNIQUESUBJECTDNSERIALNUMBER));
                final boolean useCertReqHistory = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECERTREQHISTORY));
                final boolean useUserStorage = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEUSERSTORAGE));
                final boolean useCertificateStorage = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECERTIFICATESTORAGE));
                final String approvalSettingValues = requestMap.get(SELECT_APPROVALSETTINGS);//request.getParameterValues(SELECT_APPROVALSETTINGS);
                final String numofReqApprovalsParam = requestMap.get(SELECT_NUMOFREQUIREDAPPROVALS);
                final String availablePublisherValues = requestMap.get(SELECT_AVAILABLECRLPUBLISHERS);//request.getParameterValues(SELECT_AVAILABLECRLPUBLISHERS);
                final boolean useauthoritykeyidentifier = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_AUTHORITYKEYIDENTIFIER));
                final boolean authoritykeyidentifiercritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL));
                final boolean usecrlnumber = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECRLNUMBER));
                final boolean crlnumbercritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_CRLNUMBERCRITICAL));
                final String defaultcrldistpoint = requestMap.get(TEXTFIELD_DEFAULTCRLDISTPOINT);
                final String defaultcrlissuer = requestMap.get(TEXTFIELD_DEFAULTCRLISSUER);
                final String defaultocsplocator  = requestMap.get(TEXTFIELD_DEFAULTOCSPLOCATOR);
                final String authorityInformationAccess = requestMap.get(TEXTFIELD_AUTHORITYINFORMATIONACCESS);
                final String caDefinedFreshestCrl = requestMap.get(TEXTFIELD_CADEFINEDFRESHESTCRL);
                final boolean useutf8policytext = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEUTF8POLICYTEXT));
                final boolean useprintablestringsubjectdn = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USEPRINTABLESTRINGSUBJECTDN));
                final boolean useldapdnorder = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USELDAPDNORDER));
                final boolean usecrldistpointoncrl = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_USECRLDISTRIBUTIONPOINTONCRL));
                final boolean crldistpointoncrlcritical = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_CRLDISTRIBUTIONPOINTONCRLCRITICAL));
                final boolean includeInHealthCheck = Boolean.valueOf(requestMap.get(HIDDEN_INCLUDEINHEALTHCHECK));
                final boolean serviceOcspActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATEOCSPSERVICE));
                final boolean serviceXkmsActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATEXKMSSERVICE));
                final boolean serviceCmsActive = CHECKBOX_VALUE.equals(requestMap.get(CHECKBOX_ACTIVATECMSSERVICE));
                final String sharedCmpRaSecret = requestMap.get(TEXTFIELD_SHAREDCMPRASECRET);
                final String nameConstraintsPermitted = requestMap.get(TEXTFIELD_NAMECONSTRAINTSPERMITTED);
                final String nameConstraintsExcluded = requestMap.get(TEXTFIELD_NAMECONSTRAINTSEXCLUDED);
                final String subjectdn;
                final String signedByString;
                if (cadatahandler.getCAInfo(caid).getCAInfo().getStatus() == CAConstants.CA_UNINITIALIZED) {
                    subjectdn = requestMap.get(TEXTFIELD_SUBJECTDN);
                    signedByString = requestMap.get(SELECT_SIGNEDBY);
                } else {
                    CAInfo cainfo = cadatahandler.getCAInfo(caid).getCAInfo();
                    subjectdn = cainfo.getSubjectDN();
                    signedByString = String.valueOf(cainfo.getSignedBy());
                }
                final CAInfo cainfo = cabean.createCaInfo(caid, caname, subjectdn, catype,
            		keySequenceFormatParam, keySequence, signedByString, description, validityString,
            		crlperiod, crlIssueInterval, crlOverlapTime, deltacrlperiod, finishUser,
            		isDoEnforceUniquePublicKeys, isDoEnforceUniqueDistinguishedName, isDoEnforceUniqueSubjectDNSerialnumber,
            		useCertReqHistory, useUserStorage, useCertificateStorage, approvalSettingValues, numofReqApprovalsParam,
            		availablePublisherValues, useauthoritykeyidentifier, authoritykeyidentifiercritical, usecrlnumber,
            		crlnumbercritical, defaultcrldistpoint, defaultcrlissuer, defaultocsplocator, authorityInformationAccess,
            		nameConstraintsPermitted, nameConstraintsExcluded,
            		caDefinedFreshestCrl, useutf8policytext, useprintablestringsubjectdn, useldapdnorder, usecrldistpointoncrl,
            		crldistpointoncrlcritical, includeInHealthCheck, serviceOcspActive, serviceXkmsActive, serviceCmsActive, sharedCmpRaSecret
            		);
                
                if (cadatahandler.getCAInfo(caid).getCAInfo().getStatus() == CAConstants.CA_UNINITIALIZED) {
                    // Allow changing of subjectDN etc. for uninitialized CAs
                    if (validityString != null) {
                        final long validity = ValidityDate.encode(validityString);
                        if (validity<0) {
                            throw new ParameterException(ejbcawebbean.getText("INVALIDVALIDITYORCERTEND"));
                        }
                        cainfo.setValidity(validity);
                    }
                    cainfo.setSubjectDN(subjectdn);
                    
                    // We can only update the CAToken properties if we have selected a valid cryptotoken
                    final String cryptoTokenIdString = requestMap.get(HIDDEN_CACRYPTOTOKEN);
                    if (!StringUtils.isEmpty(cryptoTokenIdString)) {
                        signatureAlgorithmParam = requestMap.get(HIDDEN_CASIGNALGO);
                        final int cryptoTokenId = Integer.parseInt(cryptoTokenIdString);
                        final String keyAliasCertSignKey = requestMap.get(SELECT_CRYPTOTOKEN_CERTSIGNKEY);
                        final String keyAliasCrlSignKey = keyAliasCertSignKey; // see comment about crlSignKey in editcapage.jspf
                        final String keyAliasDefaultKey = requestMap.get(SELECT_CRYPTOTOKEN_DEFAULTKEY);
                        final String keyAliasHardTokenEncryptKey = requestMap.get(SELECT_CRYPTOTOKEN_HARDTOKENENCRYPTKEY);
                        final String keyAliasKeyEncryptKey = requestMap.get(SELECT_CRYPTOTOKEN_KEYENCRYPTKEY);
                        final String keyAliasKeyTestKey = requestMap.get(SELECT_CRYPTOTOKEN_KEYTESTKEY);
                        final Properties caTokenProperties = new Properties();
                        caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_DEFAULT_STRING, keyAliasDefaultKey);
                        if (keyAliasCertSignKey.length()>0) {
                            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_CERTSIGN_STRING, keyAliasCertSignKey);
                        }
                        if (keyAliasCrlSignKey.length()>0) {
                            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_CRLSIGN_STRING, keyAliasCrlSignKey);
                        }
                        if (keyAliasHardTokenEncryptKey.length()>0) {
                            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_HARDTOKENENCRYPT_STRING, keyAliasHardTokenEncryptKey);
                        }
                        if (keyAliasKeyEncryptKey.length()>0) {
                            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_KEYENCRYPT_STRING, keyAliasKeyEncryptKey);
                        }
                        if (keyAliasKeyTestKey.length()>0) {
                            caTokenProperties.setProperty(CATokenConstants.CAKEYPURPOSE_TESTKEY_STRING, keyAliasKeyTestKey);
                        }
                        
                        final CAToken newCAToken = new CAToken(cryptoTokenId, caTokenProperties);
                        newCAToken.setSignatureAlgorithm(signatureAlgorithmParam);
                        cainfo.setCAToken(newCAToken);
                    }
                    
                    final String certificateProfileIdString = requestMap.get(SELECT_CERTIFICATEPROFILE);
                    int certprofileid = (certificateProfileIdString==null ? 0 : Integer.parseInt(certificateProfileIdString));
                    int signedby = (signedByString==null ? 0 : Integer.parseInt(signedByString));
                    if (signedby == caid) { signedby = CAInfo.SELFSIGNED; }
                    cainfo.setCertificateProfileId(certprofileid);
                    cainfo.setSignedBy(signedby);
                    
                    final String subjectaltname = requestMap.get(TEXTFIELD_SUBJECTALTNAME);
                    if (!cabean.checkSubjectAltName(subjectaltname)) {
                        throw new ParameterException(ejbcawebbean.getText("INVALIDSUBJECTDN"));
                    }
                    
                    List<CertificatePolicy> policies = null;
                    if (cainfo instanceof X509CAInfo) {
                        policies = cabean.parsePolicies(requestMap.get(TEXTFIELD_POLICYID));
                    }
                    
                    List<ExtendedCAServiceInfo> extendedcaservices = null;
                    if (cainfo instanceof X509CAInfo) {
                        X509CAInfo x509cainfo = (X509CAInfo)cainfo;
                        final String signkeyspec = requestMap.containsKey(SELECT_KEYSIZE) ?
                                requestMap.get(SELECT_KEYSIZE) : requestMap.get(HIDDEN_KEYSIZE);
                        extendedcaservices = cabean.makeExtendedServicesInfos(signkeyspec, cainfo.getSubjectDN(), serviceXkmsActive, serviceCmsActive);
                        x509cainfo.setExtendedCAServiceInfos(extendedcaservices);
                        x509cainfo.setSubjectAltName(subjectaltname);
                        x509cainfo.setPolicies(policies);
                    }
                }
                
                if (requestMap.get(BUTTON_SAVE) != null) {
                    // Save the CA info but do nothing More
                    cadatahandler.editCA(cainfo);
                }
                
                if (requestMap.get(BUTTON_INITIALIZE) != null) {
                    final String certificateProfileIdString = requestMap.get(SELECT_CERTIFICATEPROFILE);
                    int certprofileid = (certificateProfileIdString==null ? 0 : Integer.parseInt(certificateProfileIdString));
                    int signedby = (signedByString==null ? 0 : Integer.parseInt(signedByString));
                    cainfo.setSignedBy(signedby);
                    cainfo.setCertificateProfileId(certprofileid);
                    try {
                        cadatahandler.initializeCA(cainfo);
                    } catch (CryptoTokenOfflineException ctoe) {
                        initcatokenoffline = true;
                        errormessage = ctoe.getMessage();
                        includefile="choosecapage.jspf";
                    }
                }
                
                // Make Request Button Pushed down, this will create a certificate request but not do anything
                // else with the CA. For creating cross-certificate requests of similar.
                if (requestMap.get(BUTTON_MAKEREQUEST) != null) {
                    final String nextSignKeyAlias = requestMap.get(SELECT_CRYPTOTOKEN_CERTSIGNKEY_MAKEREQUEST);
                    byte[] certreq = cadatahandler.makeRequest(caid, fileBuffer, nextSignKeyAlias);
                	cabean.saveRequestData(certreq);
                    filemode = CERTREQGENMODE;
                    includefile = "displayresult.jspf";
                }
            }
            if (requestMap.get(BUTTON_CANCEL) != null) {
               // Don't save changes.
            }
        }
        
        // Create an authenticated (extra signature) CSR from regular CSR
        if (ACTION_SIGNREQUEST.equals(action) && !buttoncancel) {
        	try {
            	if (cabean.createAuthCertSignRequest(caid, fileBuffer)) {
                	filemode = CERTREQGENMODE;
                    includefile = "displayresult.jspf";
                }
            } catch (Exception e) {
            	errormessage = e.getMessage();
            }
        }
        if (ACTION_CHOOSE_CATYPE.equals(action)) {
    	    // Change the CA type we are
    	    catype = Integer.parseInt(requestMap.get(SELECT_CATYPE));
            caname = requestMap.get(HIDDEN_CANAME);   
            keySequenceFormat = StringTools.KEY_SEQUENCE_FORMAT_NUMERIC;
            if (requestMap.get(SELECT_KEY_SEQUENCE_FORMAT) != null) {
          	    keySequenceFormat = Integer.parseInt(requestMap.get(SELECT_KEY_SEQUENCE_FORMAT));
            }
            editca = (caid != 0);
            includefile="editcapage.jspf";
        }
        if (ACTION_CHOOSE_CATOKENTYPE.equals(action)) {
            cryptoTokenIdParam = request.getParameter(SELECT_CRYPTOTOKEN);
            signatureAlgorithmParam = request.getParameter(HIDDEN_CASIGNALGO);
            catype = Integer.parseInt(request.getParameter(HIDDEN_CATYPE));
            caname = request.getParameter(HIDDEN_CANAME);   
            keySequenceFormat = StringTools.KEY_SEQUENCE_FORMAT_NUMERIC;
            if (request.getParameter(SELECT_KEY_SEQUENCE_FORMAT) != null) {
            	keySequenceFormat = Integer.parseInt(request.getParameter(SELECT_KEY_SEQUENCE_FORMAT)); 
            }
            editca = (caid != 0);
            includefile="editcapage.jspf";
        }
        if (ACTION_CHOOSE_CASIGNALGO.equals(action)) {
            cryptoTokenIdParam = requestMap.get(SELECT_CRYPTOTOKEN);
    	    signatureAlgorithmParam = requestMap.get(SELECT_SIGNATUREALGORITHM);
            catype = Integer.parseInt(requestMap.get(HIDDEN_CATYPE));
            caname = requestMap.get(HIDDEN_CANAME);   
            keySequenceFormat = StringTools.KEY_SEQUENCE_FORMAT_NUMERIC;
            if (requestMap.get(SELECT_KEY_SEQUENCE_FORMAT) != null) {
          	    keySequenceFormat = Integer.parseInt(requestMap.get(SELECT_KEY_SEQUENCE_FORMAT)); 
            }
            editca = (caid != 0);
            includefile="editcapage.jspf";              
        }
        if (ACTION_IMPORTCA.equals(action)) {
            if (!buttoncancel) {
                try {
                    cadatahandler.importCAFromKeyStore(importcaname, fileBuffer, importpassword, importsigalias, importencalias);
                } catch (Exception e) {
				    %> <div style="color: #FF0000;"> <%
						    out.println( e.getMessage() );
				    %> </div> <%
				    includefile="importca.jspf";
		        }
            }
        }
        if (ACTION_IMPORTCACERT.equals(action)) {
            if (!buttoncancel) {
                try {
                    // Load PEM
	                cadatahandler.importCACert(importcaname, fileBuffer);
	            } catch (Exception e) {
				    %> <div style="color: #FF0000;"> <%
						    out.println( e.getMessage() );
				    %> </div> <%
				    includefile="importcacert.jspf";
				}
            }
        }

    } catch (CryptoTokenOfflineException ctoe) {
        catokenoffline = true;
        errormessage = ctoe.getMessage();
        includefile="choosecapage.jspf";
    }
   
 // Include page
  if( includefile.equals("editcapage.jspf")) {
%>
   <%@ include file="editcapage.jspf" %>
<%}
  if( includefile.equals("choosecapage.jspf")){ %>
   <%@ include file="choosecapage.jspf" %> 
<%}  
  if( includefile.equals("recievefile.jspf")){ %>
   <%@ include file="recievefile.jspf" %> 
<%} 
  if( includefile.equals("displayresult.jspf")){ %>
   <%@ include file="displayresult.jspf" %> 
<%}
  if( includefile.equals("importca.jspf")){ %>
   <%@ include file="importca.jspf" %> 
<%}
  if( includefile.equals("importcacert.jspf")){ %>
   <%@ include file="importcacert.jspf" %> 
<%}

   // Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />

</body>
</html>
