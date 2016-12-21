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
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cesecore.audit.impl.integrityprotected.AuditRecordData;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.roles.RoleData;
import org.ejbca.util.CliTools;

/**
 * Export database table(s) and verify integrity protection
 *  
 * @version $Id: DatabaseExportCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseExportCommand extends DatabaseCliCommand {

    private static final Logger LOG = Logger.getLogger(DatabaseExportCommand.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String DEFAULT_PU = "ejbca-read";
    private static final String ARG_ALL = "all";
    private static final String OUTPUT_FLAG = "-output";

    @Override
    public String getDescription() {
        return "Export database table(s) to file.";
    }

    @Override
    public String getMainCommand() {
        return null;
    }

    @Override
    public String getSubCommand() {
        return "export";
    }

    @Override
    public void execute(String[] args) {
        if (args.length<3) {
            LOG.info(getSubCommand() + " <" + ARG_ALL + " | EntityName> <output directory> [persistence unit (default is '"
                    + DEFAULT_PU + "')]" + "[verify integrity protection: true|false (default is false)]" + "[" + OUTPUT_FLAG + " "
                    + OutputFormat.getFormattedFormatNames() + " (default is binary)]");
            return;
        }
        List<String> argsList = CliTools.getAsModifyableList(args);
        OutputFormat outputFormat;
        int outputFormatIndex = argsList.indexOf(OUTPUT_FLAG);
        if( outputFormatIndex != -1) {
            String outputFormatName = argsList.get(outputFormatIndex+1);
            argsList.remove(outputFormatIndex+1);
            argsList.remove(outputFormatIndex);         
            outputFormat = OutputFormat.reversLookupByFormatName(outputFormatName);
            if(outputFormat == null) {
                outputFormat = OutputFormat.BINARY;
            }
        } else {
            outputFormat = OutputFormat.BINARY;
        }
        args = argsList.toArray(new String[argsList.size()]);
        
        final String directory = args[2];
        final String persistenceUnit = args.length>=4 ? args[3] : DEFAULT_PU;
        final boolean verifyIntegrity = args.length==5 ? Boolean.TRUE.toString().equalsIgnoreCase(args[4]) : false;
        if (!new File(directory).isDirectory()) {
            LOG.error(directory + " is not a valid directory.");
            return;
        }
        exportTables(args[1], directory, persistenceUnit, verifyIntegrity, outputFormat);
    }

    private void exportTables(final String entityClass, final String directory, final String persistenceUnit, final boolean verifyIntegrity,
            final OutputFormat format) {
        final boolean all = ARG_ALL.equalsIgnoreCase(entityClass);
        for (final Entry<Class<?>,String[]> entry : getEntityClasses(entityClass).entrySet()) {
            if (all || entry.getKey().getSimpleName().equals(entityClass)) {
                if (all && (entry.getKey().equals(AccessRuleData.class) || entry.getKey().equals(AccessUserAspectData.class))) {
                    LOG.info(entry.getKey().getSimpleName() + " is handled as part of " + RoleData.class.getSimpleName() + ". Skipping explicit export.");
                    continue;   // Skip 
                }
                final File f = new File(directory, entry.getKey().getSimpleName() + format.getFileEnding());
                if (entry.getKey().equals(AuditRecordData.class)) {
                    LOG.info("Node and sequence number chains of "+ AuditRecordData.class.getSimpleName() + " are not validated during export.");
                }
                exportTable(entry.getKey(), entry.getValue(), DEFAULT_BATCH_SIZE, f, persistenceUnit, verifyIntegrity, format);
            }
        }
    }
}


