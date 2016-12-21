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
package org.ejbca.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.roles.RoleData;

/**
 * Helper for writing a database table and create integrity protection
 * if configured to.
 *  
 * @version $Id: DatabaseWriter.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseWriter<T> {
    
    private static final Logger LOG = Logger.getLogger(DatabaseWriter.class);
    
    private final EntityManager entityManager;
    private int totalRowCount = 0;

    public DatabaseWriter(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    public boolean writeNextChunk(final List<T> entities) {
        boolean addedSomething = false;
        boolean rollBack = false;
        /*//final EntityTransaction transaction = entityManager.getTransaction();
        if (entityManager.getTransaction().isActive()) {
            LOG.error("Transaction was already active.");
        } else {*/
        if (entities.size()>0) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
            
            try {
                for (final T entity : entities) {
                    //LOG.debug("About to persist a " + entity.getClass().getSimpleName());

                    if (RoleData.class.equals(entity.getClass())) {
                        final RoleData roleData = ((RoleData)entity);
                        final Set<Integer> accessRulesPrimaryKeys = roleData.getAccessRules().keySet();
                        final Set<Integer> accessUserAspectPrimaryKeys = roleData.getAccessUsers().keySet();
                        final Map<Integer, AccessRuleData> accessRuleMap = new HashMap<Integer,AccessRuleData>();
                        for (int primaryKey : accessRulesPrimaryKeys) {
                            AccessRuleData accessRuleData = entityManager.find(AccessRuleData.class, primaryKey);
                            if (accessRuleData == null) {
                                accessRuleData = roleData.getAccessRules().get(primaryKey);
                                entityManager.persist(accessRuleData);
                            }
                            accessRuleMap.put(primaryKey, accessRuleData);
                        }
                        final Map<Integer, AccessUserAspectData> accessUserAspectMap = new HashMap<Integer,AccessUserAspectData>();
                        for (int primaryKey : accessUserAspectPrimaryKeys) {
                            AccessUserAspectData accessUserAspectData = entityManager.find(AccessUserAspectData.class, primaryKey);
                            if (accessUserAspectData == null) {
                                accessUserAspectData = roleData.getAccessUsers().get(primaryKey);
                                entityManager.persist(accessUserAspectData);
                            }
                            accessUserAspectMap.put(primaryKey, accessUserAspectData);
                        }
                        //roleData.setAccessRules(new HashMap<Integer,AccessRuleData>());
                        //roleData.setAccessUsers(new HashMap<Integer,AccessUserAspectData>());
                        roleData.setAccessRules(accessRuleMap);
                        roleData.setAccessUsers(accessUserAspectMap);
                        entityManager.persist(entity);
                        //roleData.setAccessRules(accessRuleMap);
                        //roleData.setAccessUsers(accessUserAspectMap);
                        //entityManager.merge(entity);
                    } else {
                        entityManager.persist(entity);
                    }
                    addedSomething = true;
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                rollBack = true;
            } finally {
                if (rollBack) {
                    transaction.rollback();
                } else {
                    transaction.commit();
                    totalRowCount+=entities.size();
                }
            }
        //}
            entityManager.clear();  // Detach all entities to free up some memory.
        }
        return addedSomething;
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }
}
