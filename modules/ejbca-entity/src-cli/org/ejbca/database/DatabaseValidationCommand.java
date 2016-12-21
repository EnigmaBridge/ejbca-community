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

import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.cesecore.audit.audit.AuditLogValidationReport;
import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.ejbca.database.audit.IntegrityProtectedAuditReader;

/**
 * Validation of database integrity protection.
 *  
 * @version $Id: DatabaseValidationCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseValidationCommand extends DatabaseCliCommand {

    private static final Logger LOG = Logger.getLogger(DatabaseValidationCommand.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String DEFAULT_PU = "ejbca-read";
    private static final String ARG_ALL = "all";

    @Override
    public String getDescription() {
        return "Perform database integrity protection validation";
    }

    @Override
    public String getMainCommand() {
        return null;
    }

    @Override
    public String getSubCommand() {
        return "verify";
    }

    @Override
    public void execute(String[] args) {
        if (args.length<2) {
            LOG.info(getSubCommand() + " <"+ARG_ALL+" | EntityName> [persistence unit (default is '"+DEFAULT_PU+"')]");
            return;
        }
        final String persistenceUnit = args.length==3 ? args[2] : DEFAULT_PU;
        validateTables(args[1], persistenceUnit);
    }

    public void validateTables(final String entityClass, final String persistenceUnit) {
        final EntityManager entityManager = getEntityManager(persistenceUnit);
        final boolean all = ARG_ALL.equalsIgnoreCase(entityClass);
        for (final Entry<Class<?>,String[]> entry : getEntityClasses(entityClass).entrySet()) {
            if (all || entry.getKey().getSimpleName().equals(entityClass)) {
                if (entry.getKey().equals(AuditRecordData.class)) {
                    validateAuditLog(entityManager);       
                } else {
                    validateTable(entry.getKey(), entry.getValue(), DEFAULT_BATCH_SIZE, entityManager);
                }
            }
        }
    }
    
    /**
     * Read every single entry from a table to trigger database protection
     * @param c The JPA entity
     * @param primaryKey the column queries will be ordered by
     * @param batchSize the number of entries to fetch at the time
     */
    public <T> void validateTable(final Class<T> c, final String[] primaryKey, final int batchSize, final EntityManager entityManager) {
        final DatabaseReader<T> dbReader = new DatabaseReader<T>(c, primaryKey, batchSize, false, 1000, true);
        do {
            LOG.info(c.getSimpleName() + ": " + dbReader.getTotalRowCount() + " rows validated so far.");
            dbReader.getNextVerifiedChunk(entityManager); // We just throw away the read data here..
        } while (!dbReader.isDone());
        LOG.info(c.getSimpleName() + ": " + (dbReader.getTotalRowCount()-dbReader.getErrorCount()) + "/" + dbReader.getTotalRowCount() + " ok.");
    }

    public AuditLogValidationReport validateAuditLog(final EntityManager entityManager) {
        final long startTime = System.currentTimeMillis();
        long rowCount = 0;
        final IntegrityProtectedAuditReader ipar = new IntegrityProtectedAuditReader(entityManager, 0, System.currentTimeMillis(), 10000);
        while ( true ) {
        	final int chunkLength = ipar.getNextVerifiedChunk();
        	if ( ipar.isDone() ) {
        		break;
        	}
        	rowCount += chunkLength;
        	if (rowCount>0) {
        		LOG.info("Progress: node=" + ipar.getNodeId() + " rowCount=" + rowCount);
        	} else {
        		LOG.info("Progress: no valid entries found so far!");
        	}
        }
        final AuditLogValidationReport auditLogValidationReport = ipar.getAuditLogValidationReport();
        LOG.info("Audit log validation completed in " + (System.currentTimeMillis()-startTime)/1000 + " seconds. " + rowCount
                + " rows found. Errors: " + auditLogValidationReport.errors().size() + " Warnings: " + auditLogValidationReport.warnings().size());
        return auditLogValidationReport;
    }
}
