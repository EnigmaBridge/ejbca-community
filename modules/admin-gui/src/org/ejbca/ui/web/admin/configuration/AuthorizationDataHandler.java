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

package org.ejbca.ui.web.admin.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.rules.AccessRuleNotFoundException;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.roles.RoleData;
import org.cesecore.roles.RoleExistsException;
import org.cesecore.roles.RoleNotFoundException;
import org.cesecore.roles.access.RoleAccessSession;
import org.cesecore.roles.management.RoleManagementSession;
import org.ejbca.core.model.InternalEjbcaResources;

/**
 * A class handling the authorization data.
 * 
 * FIXME: Rename the methods in this class to fit what they actually do
 * 
 * @version $Id: AuthorizationDataHandler.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class AuthorizationDataHandler implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Internal localization of logs and errors */
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    private AccessControlSessionLocal authorizationsession;
    private RoleAccessSession roleAccessSession;
    private RoleManagementSession roleManagementSession;
    private AuthenticationToken administrator;
    private Collection<RoleData> authorizedRoles;
    private InformationMemory informationmemory;

    /** Creates a new instance of ProfileDataHandler */
    public AuthorizationDataHandler(AuthenticationToken administrator, InformationMemory informationmemory, RoleAccessSession roleAccessSession,
            RoleManagementSession roleManagementSession, AccessControlSessionLocal authorizationsession) {
        this.roleManagementSession = roleManagementSession;
        this.roleAccessSession = roleAccessSession;
        this.authorizationsession = authorizationsession;
        this.administrator = administrator;
        this.informationmemory = informationmemory;
    }

    /**
     * Method to check if a admin is authorized to a resource
     * 
     * @param admin information about the administrator to be authorized.
     * @param resource the resource to look up.
     * @return true if authorizes
     * @throws AuthorizationDeniedException when authorization is denied.
     */
    public boolean isAuthorized(AuthenticationToken admin, String... resources) {
        return authorizationsession.isAuthorized(admin, resources);
    }

    /**
     * Method to check if a admin is authorized to a resource without performing any logging.
     * 
     * @param admin information about the administrator to be authorized.
     * @param resource the resource to look up.
     * @return true if authorizes
     * @throws AuthorizationDeniedException when authorization is denied.
     */
    public boolean isAuthorizedNoLog(AuthenticationToken admin, String... resources) {
        return authorizationsession.isAuthorizedNoLogging(admin, resources);
    }

    /**
     * Method to add a new role to the administrator privileges data.
     * 
     * @throws RoleExistsException
     */
    public void addRole(String name) throws AuthorizationDeniedException, RoleExistsException {
        // Authorized to edit administrative privileges
        if (!authorizationsession.isAuthorized(administrator, "/system_functionality/edit_administrator_privileges")) {
            final String msg = intres.getLocalizedMessage("authorization.notuathorizedtoresource",
                    "/system_functionality/edit_administrator_privileges", null);
            throw new AuthorizationDeniedException(msg);
        }
        roleManagementSession.create(administrator, name);
        informationmemory.administrativePriviledgesEdited();
        this.authorizedRoles = null;
    }

    /**
     * Method to remove a role.
     * 
     * @throws RoleNotFoundException
     */
    public void removeRole(String name) throws AuthorizationDeniedException, RoleNotFoundException {
        roleManagementSession.remove(administrator, name);
        informationmemory.administrativePriviledgesEdited();
        this.authorizedRoles = null;
    }

    /**
     * Method to rename a role.
     * 
     * @throws RoleExistsException
     */
    public void renameRole(String oldname, String newname) throws AuthorizationDeniedException, RoleExistsException {
        roleManagementSession.renameRole(administrator, oldname, newname);
        informationmemory.administrativePriviledgesEdited();
        this.authorizedRoles = null;
    }

    /**
     * Method returning a Collection of authorized roles. Only the fields role name and CA id is filled in these objects.
     */
    public Collection<RoleData> getRoles() {
        if (this.authorizedRoles == null) {
            this.authorizedRoles = roleManagementSession.getAllRolesAuthorizedToEdit(administrator);
        }
        return this.authorizedRoles;
    }

    /**
     * @return the given role with it's authorization data
     * 
     */
    public RoleData getRole(String roleName) {
        return roleAccessSession.findRole(roleName);
    }

    /**
     * Method to add a Collection of AccessRule to an role.
     * 
     * @throws AuthorizationDeniedException when administrator is't authorized to edit this CA or when administrator tries to add access rules she
     *             isn't authorized to.
     * @throws RoleNotFoundException
     * @throws AccessRuleNotFoundException
     */
    public void addAccessRules(String roleName, Collection<AccessRuleData> accessRules) throws AuthorizationDeniedException,
            AccessRuleNotFoundException, RoleNotFoundException {
        roleManagementSession.addAccessRulesToRole(administrator, roleAccessSession.findRole(roleName), accessRules);
        informationmemory.administrativePriviledgesEdited();
    }

    /**
     * Method to remove an collection of access rules from a role.
     * 
     * @param accessrules a Collection of AccessRuleData containing accesss rules to remove.
     * @throws AuthorizationDeniedException when administrator is't authorized to edit this CA.
     * @throws RoleNotFoundException
     */
    public void removeAccessRules(String roleName, Collection<AccessRuleData> accessRules) throws AuthorizationDeniedException, RoleNotFoundException {
        RoleData role = roleAccessSession.findRole(roleName);
        Collection<AccessRuleData> rulesToRemove = new ArrayList<AccessRuleData>();
        for(AccessRuleData rule : accessRules) {
            if(role.getAccessRules().containsKey(rule.getPrimaryKey())) {
                rulesToRemove.add(rule);
            }
        }
        roleManagementSession.removeAccessRulesFromRole(administrator, role, rulesToRemove);
        informationmemory.administrativePriviledgesEdited();
    }

    /**
     * Method to replace an collection of access rules in a role.
     * 
     * @param rolename the name of the given role
     * @param accessrules a Collection of String containing accesssrules to replace.
     * @throws AuthorizationDeniedException when administrator is't authorized to edit this CA.
     * @throws RoleNotFoundException if role of given name wasn't found
     */
    public void replaceAccessRules(String rolename, Collection<AccessRuleData> accessRules) throws AuthorizationDeniedException, RoleNotFoundException {
        roleManagementSession.replaceAccessRulesInRole(administrator, roleAccessSession.findRole(rolename), accessRules);
        informationmemory.administrativePriviledgesEdited();
    }

    /**
     * Method returning all the available access rules authorized to administrator to manage.
     * 
     * @returns a map of sets of strings with available access rules, sorted by category
     */
    public Map<String, Set<String>> getAvailableAccessRules() {
        return this.informationmemory.getAuthorizedAccessRules();
    }

    /**
     * Method returning all the available access rules authorized to administrator to manage.
     * 
     * @returns a Collection of strings with available access rules.
     */
    public Set<String> getAvailableAccessRulesUncategorized() {
        return this.informationmemory.getAuthorizedAccessRulesUncategorized();
    }
    
    /**
     * Method to add a Collection of AdminEntity to an role.
     * 
     * @throws AuthorizationDeniedException if administrator isn't authorized to edit CAs administrative privileges.
     * @throws RoleNotFoundException 
     */
    public void addAdminEntities(RoleData role, Collection<AccessUserAspectData> subjects) throws AuthorizationDeniedException, RoleNotFoundException {
        roleManagementSession.addSubjectsToRole(administrator, role, subjects);
        informationmemory.administrativePriviledgesEdited();
    }

    /**
     * Method to remove a Collection of AdminEntity from an role.
     * 
     * @throws AuthorizationDeniedException if administrator isn't authorized to edit CAs administrative privileges.
     * @throws RoleNotFoundException 
     */
    public void removeAdminEntities(RoleData role, Collection<AccessUserAspectData> subjects) throws AuthorizationDeniedException, RoleNotFoundException {
        roleManagementSession.removeSubjectsFromRole(administrator, role, subjects);
        informationmemory.administrativePriviledgesEdited();
    }

}
