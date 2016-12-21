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
package org.cesecore.authorization.control;

import java.util.List;

import org.cesecore.authorization.rules.AccessRulePlugin;

/**
 * @version $Id: AuditLogRulesReference.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class AuditLogRulesReference implements AccessRulePlugin{

    @Override
    public List<String> getRules() {
        return AuditLogRules.getAllResources();
    }

    @Override
    public String getCategory() {
        return "AUDITLOGRULES";
    }

}
