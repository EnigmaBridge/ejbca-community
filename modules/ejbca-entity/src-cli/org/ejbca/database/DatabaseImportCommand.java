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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cesecore.authorization.rules.AccessRuleData;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.roles.RoleData;

/**
 * Writing database table(s) and create integrity protection if configured to.
 *  
 * @version $Id: DatabaseImportCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseImportCommand extends DatabaseCliCommand {

    private static final Logger LOG = Logger.getLogger(DatabaseImportCommand.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String DEFAULT_PU = "ejbca-write";
    private static final String ARG_ALL = "all";

    @Override
    public String getDescription() {
        return "Import database table(s) to file.";
    }

    @Override
    public String getMainCommand() {
        return null;
    }

    @Override
    public String getSubCommand() {
        return "import";
    }

    @Override
    public void execute(String[] args) {
        if (args.length<3) {
            LOG.info(getSubCommand() + " <"+ARG_ALL+" | EntityName> <input directory> [persistence unit (default is '"+DEFAULT_PU+"')]");
            return;
        }
        final String directory = args[2];
        final String persistenceUnit = args.length==4 ? args[3] : DEFAULT_PU;
        if (!new File(directory).isDirectory()) {
            LOG.error(directory + " is not a valid directory.");
            return;
        }
        importTables(args[1], directory, persistenceUnit);
    }

    private void importTables(final String entityClass, final String directory, final String persistenceUnit) {
        final boolean all = ARG_ALL.equalsIgnoreCase(entityClass);
        for (final Entry<Class<?>,String[]> entry : getEntityClasses(entityClass).entrySet()) {
            if (all || entry.getKey().getSimpleName().equals(entityClass)) {
                if (all && (entry.getKey().equals(AccessRuleData.class) || entry.getKey().equals(AccessUserAspectData.class))) {
                    LOG.info(entry.getKey().getSimpleName() + " is handled as part of " + RoleData.class.getSimpleName() + ". Skipping explicit import.");
                    continue;   // Skip 
                }
                final File f = new File(directory, entry.getKey().getSimpleName() + ".bin");
                if (!f.exists()) {
                    LOG.warn("Skipping " + entry.getKey().getSimpleName() + " since " + f.getAbsolutePath() + " does not exist.");
                    continue;
                }
                importTable(entry.getKey(), entry.getValue(), DEFAULT_BATCH_SIZE, f, persistenceUnit);
            }
        }
    }
}
