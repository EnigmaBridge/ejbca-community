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

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.certificates.crl.CRLData;
import org.cesecore.roles.RoleData;

/**
 * Export and import of database table(s) through temporary files.
 *  
 * @version $Id: DatabaseCopyCommand.java 20429 2014-12-09 15:17:08Z anatom $
 */
public class DatabaseCopyCommand  extends DatabaseCliCommand {

    private static final Logger LOG = Logger.getLogger(DatabaseCopyCommand.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String DEFAULT_PU_IN = "ejbca-read";
    private static final String DEFAULT_PU_OUT = "ejbca-write";
    private static final String ARG_ALL = "all";

    @Override
    public String getDescription() {
        return "Copy database table(s) from one persistence unit to another.";
    }

    @Override
    public String getMainCommand() {
        return null;
    }

    @Override
    public String getSubCommand() {
        return "copy";
    }

    @Override
    public void execute(String[] args) {
        if (args.length<2) {
            LOG.info(getSubCommand() + " <"+ARG_ALL+" | EntityName> [input persistence unit (default is '"+
                    DEFAULT_PU_IN+"')]  [output persistence unit (default is '"+DEFAULT_PU_OUT+"')] [verify integrity protection: true|false (default is false)]");
            return;
        }
        final String persistenceUnitIn = args.length>=3 ? args[2] : DEFAULT_PU_IN;
        final String persistenceUnitOut = args.length>=4 ? args[3] : DEFAULT_PU_OUT;
        final boolean verifyIntegrity = args.length==5 ? Boolean.TRUE.toString().equalsIgnoreCase(args[4]) : false;
        copyTables(args[1], persistenceUnitIn, persistenceUnitOut, verifyIntegrity);
    }

    private void copyTables(final String entityClass, final String persistenceUnitIn, final String persistenceUnitOut, final boolean verifyIntegrity) {
        final boolean all = ARG_ALL.equalsIgnoreCase(entityClass);
        for (final Entry<Class<?>,String[]> entry : getEntityClasses(entityClass).entrySet()) {
            if (all || entry.getKey().getSimpleName().equals(entityClass)) {
                if (all && (entry.getKey().equals(AccessRuleData.class) || entry.getKey().equals(AccessUserAspectData.class))) {
                    LOG.info(entry.getKey().getSimpleName() + " is handled as part of " + RoleData.class.getSimpleName() + ". Skipping explicit import.");
                    continue;   // Skip 
                }
                try {
                    final File f = File.createTempFile(entry.getKey().getSimpleName(), ".bin");
                    if (entry.getKey().equals(AuditRecordData.class)) {
                        LOG.info("Node and sequence number chains of "+ AuditRecordData.class.getSimpleName() + " are not validated during export.");
                    }
                    final int batchSize = CRLData.class.equals(entry.getKey()) ? 20 : DEFAULT_BATCH_SIZE;   // Be defensive when handling CRLs.. they can be very large.
                    exportTable(entry.getKey(), entry.getValue(), batchSize, f, persistenceUnitIn, verifyIntegrity, OutputFormat.BINARY);
                    if (!f.exists()) {
                        LOG.warn("Skipping " + entry.getKey().getSimpleName() + " since " + f.getAbsolutePath() + " does not exist.");
                        continue;
                    }
                    importTable(entry.getKey(), entry.getValue(), batchSize, f, persistenceUnitOut);
                    f.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
