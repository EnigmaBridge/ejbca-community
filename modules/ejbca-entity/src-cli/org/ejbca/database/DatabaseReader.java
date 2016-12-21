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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.cesecore.config.ConfigurationHolder;
import org.cesecore.dbprotection.DatabaseProtectionException;

/**
 * Helper for reading a database table and verify integrity protection
 *  
 * @version $Id: DatabaseReader.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseReader<T> {

    private static final Logger log = Logger.getLogger(DatabaseReader.class);
    
    private final int maxFetchSize;
    private final List<String> errors = new ArrayList<String>();
    private boolean isDone = false;
    private final String queryFirstPk;
    private final String queryNextPk;
    private final String queryNext;
    private int totalRowCount = 0;
    private final int maxReportErrors;
    private int totalErrorCount = 0;
    private final int noPrimaryKeys;
    
    /**
     * Creates a new instance of this reader that can be used to fetch verified audit log data.
     * @param entityManager
     * @param timestampFrom
     * @param timestampTo
     * @param maxFetchSize
     */
    public DatabaseReader(final Class<T> entityClass, final String[] primaryKeys, int maxFetchSize, boolean includeFailed, int maxReportErrors, final boolean verifyIntegrity) {
        this.maxFetchSize = maxFetchSize;
        this.maxReportErrors = maxReportErrors;
        final String entityName = entityClass.getSimpleName();
        // Make sure that we really do verify and throw detectable DatabaseProtectionException if the verification fails.
        ConfigurationHolder.instance();
        ConfigurationHolder.updateConfiguration("databaseprotection.enableverify." + entityName, String.valueOf(verifyIntegrity));
        ConfigurationHolder.updateConfiguration("databaseprotection.erroronverifyfail", String.valueOf(!includeFailed));
        noPrimaryKeys = 1;
        StringBuffer sbCommas = new StringBuffer();
        StringBuffer sbAnds = new StringBuffer();
        int count = 0;
        for (String primaryKey : primaryKeys) {
            if (count>0) {
                sbCommas.append(", ");
            } else {
                sbAnds.append("a." + primaryKey+">=:primaryKey" + count);
            }
            sbCommas.append("a." + primaryKey);
            count++;
        }
        queryNext = "SELECT a FROM "+entityName+" a WHERE " + sbAnds.toString() + " ORDER BY " + sbCommas.toString();
        queryFirstPk = "SELECT "+sbCommas.toString()+" FROM "+entityName+" a ORDER BY " + sbCommas.toString();
        queryNextPk = "SELECT "+ sbCommas.toString()+" FROM "+entityName+" a WHERE " + sbAnds.toString() + " ORDER BY " + sbCommas.toString();
        if (log.isDebugEnabled()) {
            log.debug("queryNext:    " + queryNext);
            log.debug("queryFirstPk: " + queryFirstPk);
            log.debug("queryNextPk:  " + queryNextPk);
        }
    }
    
    /** @return true if there is no more data to read from this table */
    public boolean isDone() {
        return isDone;
    }
    
    /** @return a list of all the error messages. */
    public List<String> getErrors() {
        return errors;
    }

    /** @return the total number of rows where integrity protection checks failed. */
    public int getErrorCount() {
        return totalErrorCount;
    }

    /** @return the total number of rows, including the ones where integrity protection checks failed. */
    public int getTotalRowCount() {
        return totalRowCount;
    }

    
    private Object[] currentPrimaryKey;
    /**
     * Fetch and verify the next chunk of data.
     * @return a verified list of rows or an empty list if there are no more data.
     */
    public List<T> getNextVerifiedChunk(final EntityManager entityManager) {
        final List<T> ret = new ArrayList<T>();
        if (currentPrimaryKey==null) {
            currentPrimaryKey = getFirstPrimaryKey(entityManager);
        }
        if (currentPrimaryKey!=null) {
            do {
                log.debug("Before batch.. currentPrimaryKey: " + Arrays.toString(currentPrimaryKey));
                for (T entity : getNextVerifiedChunkInternal(entityManager)) {
                    ret.add(entity);
                }
                log.debug("After batch... currentPrimaryKey: " + Arrays.toString(currentPrimaryKey));
            } while (!isDone && ret.size()==0);
        } else {
            isDone = true;
        }
        return ret;
    }

    private List<T> getNextVerifiedChunkInternal(final EntityManager entityManager) {
        if (log.isDebugEnabled() && entityManager.getTransaction().isActive()) {
            log.debug("It might not be suitable to run this in a transaction, since these operations can easily time out and there is no need for updates.");
        }
        final List<T> ret = new ArrayList<T>();
        int errorCount = 0;
        try {
            final List<T> tList = getNextBatch(entityManager, currentPrimaryKey, 0, maxFetchSize);
            for (T entity : tList) {
                ret.add(entity);
            }
            totalRowCount += tList.size();
        } catch (DatabaseProtectionException eBatch) {
            log.warn("Database integrity protection breach in batch. Checking row by row..");
            for (int i=0;i<maxFetchSize;i++) {
                try {
                    final List<T> tList = getNextBatch(entityManager, currentPrimaryKey, i, 1);
                    for (T data : tList) {  // 1 or 0
                        totalRowCount ++;
                        // Since we might already have created this object with our last query, we need to verify it again using data.verifyData()
                        try {
                            // data.verifyData() is protected so we need to use reflection to invoke it
                            final Method m = data.getClass().getDeclaredMethod("verifyData");
                            m.setAccessible(true);
                            m.invoke(data);
                            // If no exception was thrown, it was ok and we add it
                            ret.add(data);
                        } catch (InvocationTargetException eVerify) {
                            throw (DatabaseProtectionException)eVerify.getTargetException();
                        } catch (Exception eVerify) {
                            log.error(eVerify.getMessage());
                            throw new DatabaseProtectionException(eVerify);
                        }
                    }
                } catch (DatabaseProtectionException eRow) {
                    // Don't let the list or errors grow without bounds
                    if (totalErrorCount<maxReportErrors) {
                        errors.add(eRow.getMessage());
                    }
                    totalErrorCount++;
                    errorCount++;
                }
            }
        }
        currentPrimaryKey = getNextPrimaryKey(entityManager, currentPrimaryKey, maxFetchSize);
        entityManager.clear();  // Detach all entities to free up some memory.
        isDone = currentPrimaryKey==null || ret.size()==0 && errorCount==0;
        return ret;
    }

    @SuppressWarnings("unchecked")
    private Object[] getFirstPrimaryKey(final EntityManager entityManager) {
        final List<Object> results = (List<Object>) entityManager.createQuery(queryFirstPk).setMaxResults(1).getResultList();
        if (results.isEmpty()) {
            return null;
        }
        Object result = results.get(0);
        Object[] primaryKeys;
        if (result instanceof Object[]) {
            primaryKeys = (Object[]) result;
        } else {
            primaryKeys = new Object[] { result };
        }
        return primaryKeys;
    }
    
    @SuppressWarnings("unchecked")
    private Object[] getNextPrimaryKey(final EntityManager entityManager, Object[] currentPrimaryKeys, int maxFetchSize) {
        final Query query = entityManager.createQuery(queryNextPk).setMaxResults(maxFetchSize+1);
        for (int i=0; i<noPrimaryKeys; i++) {
            query.setParameter("primaryKey"+i, currentPrimaryKey[i]);
        }
        final List<Object> results = (List<Object>) query.getResultList();
        if (results.size()<(maxFetchSize+1)) {
            return null;
        }
        Object result = results.get(maxFetchSize);
        Object[] primaryKeys;
        if (result instanceof Object[]) {
            primaryKeys = (Object[]) result;
        } else {
            primaryKeys = new Object[] { result };
        }
        return primaryKeys;
    }
    
    @SuppressWarnings("unchecked")
    private List<T> getNextBatch(final EntityManager entityManager, Object[] currentPrimaryKeys, int firstResult, int maxFetchSize) {
        final Query query = entityManager.createQuery(queryNext).setMaxResults(maxFetchSize).setFirstResult(firstResult);
        for (int i=0; i<noPrimaryKeys; i++) {
            query.setParameter("primaryKey"+i, currentPrimaryKey[i]);
        }
        return (List<T>) query.getResultList();
    }
}
