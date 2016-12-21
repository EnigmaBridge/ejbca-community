/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.core.model.ra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.certificates.ca.CaSession;
import org.cesecore.configuration.GlobalConfigurationSession;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.authorization.ComplexAccessControlSession;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSession;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.authorization.AccessRulesConstants;

/**
 * A class that looks up the which CA:s or end entity profiles the administrator is authorized to view.
 * 
 * @version $Id: RAAuthorization.java 20591 2015-01-23 16:00:52Z mikekushner $
 */
public class RAAuthorization implements Serializable {
    
    private static final long serialVersionUID = -3195162814492440326L;
    private String authcastring = null;
    private String authendentityprofilestring = null;
    private TreeMap<String, Integer> authprofilenames = null;
    private TreeMap<String, Integer> authcreateprofilenames = null;
	private TreeMap<String, Integer> authviewprofilenames = null;
	private List<Integer> authprofileswithmissingcas = null;
    private AuthenticationToken admin;
    private AccessControlSessionLocal authorizationsession;
    private ComplexAccessControlSession complexAccessControlSession;
    private GlobalConfigurationSession globalConfigurationSession;
    private CaSession caSession;
    private EndEntityProfileSession endEntityProfileSession;
    
    /** Creates a new instance of RAAuthorization. */
    public RAAuthorization(AuthenticationToken admin, GlobalConfigurationSession globalConfigurationSession, AccessControlSessionLocal authorizationsession, 
                    ComplexAccessControlSession complexAccessControlSession, CaSession caSession, EndEntityProfileSession endEntityProfileSession) {
    	this.admin = admin;
    	this.globalConfigurationSession = globalConfigurationSession;
    	this.authorizationsession = authorizationsession;
    	this.caSession = caSession;
    	this.endEntityProfileSession = endEntityProfileSession;
    	this.complexAccessControlSession = complexAccessControlSession;
    }

    /**
     * Method that checks the administrators CA privileges and returns a string that should be used in where clause of userdata SQL queries.
     *
     * @return a string of administrators CA privileges that should be used in the where clause of SQL queries.
     */
    public String getCAAuthorizationString() {      
      if(authcastring==null){
        Iterator<Integer> iter =  caSession.getAuthorizedCaIds(admin).iterator();
         
        authcastring = "";
        
        while(iter.hasNext()){
          if(authcastring.equals("")) {
            authcastring = " cAId = " + iter.next().toString();   
          } else {    
            authcastring = authcastring + " OR cAId = " + iter.next().toString();
          }
        }
        
        if(!authcastring.equals("")) {
          authcastring = "( " + authcastring + " )"; 
        }
      }
      
      return authcastring;
    } 

    /**
     * @return a string of end entity profile privileges that should be used in the where clause of SQL queries, or null if no authorized end entity profiles exist.
     * @throws AuthorizationDeniedException if the current requester isn't authorized to query for approvals
     */
    public String getEndEntityProfileAuthorizationString() throws AuthorizationDeniedException {
        boolean authorizedToApproveCAActions = false; // i.e approvals with endentityprofile ApprovalDataVO.ANY_ENDENTITYPROFILE
        boolean authorizedToApproveRAActions = false; // i.e approvals with endentityprofile not ApprovalDataVO.ANY_ENDENTITYPROFILE 
     
        authorizedToApproveCAActions = authorizationsession.isAuthorizedNoLogging(admin, AccessRulesConstants.REGULAR_APPROVECAACTION);

        authorizedToApproveRAActions = authorizationsession.isAuthorizedNoLogging(admin, AccessRulesConstants.REGULAR_APPROVEENDENTITY);

        if (!authorizedToApproveCAActions && !authorizedToApproveRAActions) {
            throw new AuthorizationDeniedException("Not authorized to query apporvals");
        }

    	String endentityauth = null;
        GlobalConfiguration globalconfiguration = (GlobalConfiguration) globalConfigurationSession.getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
        if (globalconfiguration.getEnableEndEntityProfileLimitations()){
        	endentityauth = getEndEntityProfileAuthorizationString(true);
        	if(authorizedToApproveCAActions && authorizedToApproveRAActions){
        		endentityauth = getEndEntityProfileAuthorizationString(true);
        		if(endentityauth != null){
        		  endentityauth = "(" + getEndEntityProfileAuthorizationString(false) + " OR endEntityProfileId=" + ApprovalDataVO.ANY_ENDENTITYPROFILE + " ) ";
        		}
        	}else if (authorizedToApproveCAActions) {
        		endentityauth = " endEntityProfileId=" + ApprovalDataVO.ANY_ENDENTITYPROFILE;
			}else if (authorizedToApproveRAActions) {
				endentityauth = getEndEntityProfileAuthorizationString(true);
			}        	
        	
        }
        return endentityauth == null ? endentityauth : endentityauth.trim();
    }

    /**
     * Method that checks the administrators end entity profile privileges and returns a string that should be used in where clause of userdata SQL queries.
     *
     * @return a string of end entity profile privileges that should be used in the where clause of SQL queries, or null if no authorized end entity profiles exist.
     */
    public String getEndEntityProfileAuthorizationString(boolean includeparanteses){
      if(authendentityprofilestring==null){
    	Collection<Integer> profileIds = new ArrayList<Integer>(endEntityProfileSession.getEndEntityProfileIdToNameMap().keySet());
      	Collection<Integer> result = this.complexAccessControlSession.getAuthorizedEndEntityProfileIds(admin, AccessRulesConstants.VIEW_END_ENTITY, profileIds);     	
      	result.retainAll(this.endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin));
      	Iterator<Integer> iter = result.iterator();
      	                    
        while(iter.hasNext()){
          if(authendentityprofilestring == null) {
            authendentityprofilestring = " endEntityProfileId = " + iter.next().toString();   
          } else {    
            authendentityprofilestring = authendentityprofilestring + " OR endEntityProfileId = " + iter.next().toString();
          }
        }
        
        if(authendentityprofilestring != null) {
          authendentityprofilestring = "( " + authendentityprofilestring + " )"; 
        }
      }
      return authendentityprofilestring; 
    }
    
    
    public TreeMap<String, Integer> getAuthorizedEndEntityProfileNames(){
    	if (authprofilenames==null){
    		authprofilenames = new TreeMap<String, Integer>();
    		final Map<Integer, String> idtonamemap = endEntityProfileSession.getEndEntityProfileIdToNameMap();
    		for (final Integer id : endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin)) {
    			authprofilenames.put(idtonamemap.get(id), id);
    		}
    	}
    	return authprofilenames;  
    }
    
	public TreeMap<String, Integer> getCreateAuthorizedEndEntityProfileNames() {
		if(authcreateprofilenames == null){
			authcreateprofilenames = getAuthorizedEndEntityProfileNames(AccessRulesConstants.CREATE_END_ENTITY);
		}
		return authcreateprofilenames;  
	}
	      
	public TreeMap<String, Integer> getViewAuthorizedEndEntityProfileNames(){
	  if(authviewprofilenames == null){
	  	  authviewprofilenames = getAuthorizedEndEntityProfileNames(AccessRulesConstants.VIEW_END_ENTITY);
	  }
	  return authviewprofilenames;
	}
	
	public List<Integer> getViewAuthorizedEndEntityProfilesWithMissingCAs() {
	   if (authprofileswithmissingcas == null) {
	       authprofileswithmissingcas = endEntityProfileSession.getAuthorizedEndEntityProfileIdsWithMissingCAs(admin);
	   }
	   return authprofileswithmissingcas;
	}
    
    public void clear(){
      authcastring=null;
      authendentityprofilestring=null;
      authprofilenames = null;
	  authcreateprofilenames = null;
	  authviewprofilenames = null;
	  authprofileswithmissingcas = null;
    }
    
    
	private TreeMap<String, Integer> getAuthorizedEndEntityProfileNames(String rights) {
		final TreeMap<String, Integer> returnval = new TreeMap<String, Integer>();
		final Map<Integer, String> profilemap = this.endEntityProfileSession.getEndEntityProfileIdToNameMap();
		for (final Integer next : endEntityProfileSession.getAuthorizedEndEntityProfileIds(admin)) {
			if (this.endEntityAuthorization(admin, next.intValue(), rights)) {
				returnval.put(profilemap.get(next), next);
			}
		}
		return returnval;
	}     
    
    /**
     * Help function used to check end entity profile authorization.
     */
    public boolean endEntityAuthorization(AuthenticationToken admin, int profileid, String rights) {
        boolean returnval = false;
        returnval = authorizationsession.isAuthorizedNoLogging(admin, AccessRulesConstants.ENDENTITYPROFILEPREFIX + Integer.toString(profileid) + rights);
        return returnval;
    }  
}


