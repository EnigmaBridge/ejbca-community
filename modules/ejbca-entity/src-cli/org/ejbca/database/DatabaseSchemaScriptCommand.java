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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;

/**
 * Helper class to generate SQL create/drop scripts using Hibernate. 
 * 
 * @version $Id: DatabaseSchemaScriptCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class DatabaseSchemaScriptCommand extends DatabaseCliCommand {

    private static final Logger LOG = Logger.getLogger(DatabaseSchemaScriptCommand.class);

    @Override
    public String getDescription() {
        return "Generate table create and drop scripts from the ORM mapping";
    }

    @Override
    public String getMainCommand() {
        return null;
    }

    @Override
    public String getSubCommand() {
        return "gen";
    }

    @Override
    public void execute(String[] args) {
        LOG.debug("Executed with " + Arrays.toString(args));
        if (args.length<3) {
            LOG.info(getSubCommand() + " <output directory> <database type:all|"+getSupportedTypeString('|')+">");
            return;
        }
        final String dir = args[1];
        if (!new File(dir).isDirectory()) {
            LOG.error(dir + " is not a directory!");
        }
        if ("all".equalsIgnoreCase(args[2])) {
            for (String databaseType : getSupportedTypeString(';').split(";")) {
                createSqlScripts(dir, databaseType);
            }
        } else  {
            createSqlScripts(dir, args[2]);
        }
    }

    private void createSqlScripts(String directory, String databaseType) {
        LOG.info("Creating scripts for " + databaseType + " in " +directory);
		final String dropScriptFileName = directory + File.separator + "drop-tables-ejbca5-" + databaseType + ".sql";
    	final String createScriptFileName = directory + File.separator + "create-tables-ejbca5-" + databaseType + ".sql";
    	final String createScriptFileNameNdb = directory + File.separator + "create-tables-ejbca5-" + databaseType + "-ndbcluster.sql";
    	// Configure with our current persistence unit
    	Properties p = new Properties();
    	p.put("hibernate.dialect", getDialect(databaseType).getName());
    	final Ejb3Configuration ejb3Configuration = new Ejb3Configuration().configure("ejbca-read", null);
    	final Configuration hibernateConfiguration = ejb3Configuration.getHibernateConfiguration();
		try {
			// Create drop script
	    	//final String[] dropScript = hibernateConfiguration.generateDropSchemaScript(Dialect.getDialect(ejb3Configuration.getProperties()));
		    final String[] dropScript = hibernateConfiguration.generateDropSchemaScript(Dialect.getDialect(p));
			StringBuilder sb = new StringBuilder();
			for (String line : dropScript) {
				sb.append(line);
				sb.append(";\n");
			}
			System.out.println("Writing drop script to " + dropScriptFileName);
			{
			FileOutputStream fileOutputStream = new FileOutputStream(dropScriptFileName);
            try {
                fileOutputStream.write(sb.toString().getBytes());
            } finally {
                fileOutputStream.close();
            }
            }
			// Create create script(s)
			final String[] createScript = hibernateConfiguration.generateSchemaCreationScript(Dialect.getDialect(p));
			sb = new StringBuilder();
			for (String line : createScript) {
				// Format nicely, so it looks more like the old, manually created ones.
				if (line.startsWith("create")) {
					line = line.replaceAll("create table", "CREATE TABLE");
					line = line.replaceAll("primary key", "PRIMARY KEY");
					line = line.replaceAll("not null", "NOT NULL");
					line = line.replaceAll("Data \\(", "Data \\(\n    ");
					line = line.replaceAll("Map \\(", "Map \\(\n    ");
					line = line.replaceAll(", ", ",\n    ");
				}
				line += ";\n\n";
				line = line.replaceAll("\\)\\);", "\\)\n\\);");
				sb.append(line);
			}
			System.out.println("Writing create script to " + createScriptFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(createScriptFileName);
            try {
                fileOutputStream.write(sb.toString().getBytes());
            } finally {
                fileOutputStream.close();
            }
			if (databaseType.equals("mysql")) {
				sb.insert(0, "-- This script assumes that the tablespace 'ejbca_ts' exists.\n\n");
				System.out.println("Writing create script to " + createScriptFileNameNdb);
				new FileOutputStream(createScriptFileNameNdb).write(sb.toString().replaceAll("\n\\);\n", "\n\\) TABLESPACE ejbca_ts STORAGE DISK ENGINE=NDB;\n").getBytes());
			}
			//String[] updateScript = hibernateConfiguration.generateSchemaUpdateScript(Dialect.getDialect(ejb3Configuration.getProperties(), ...));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
