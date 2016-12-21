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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

/**
 * Helper that searches the specified package and sub-packages for classes
 * that implement the CliCommandPlugin interface. It will also list all
 * found commands if no parameters. Otherwise it will execute the command. 
 *  
 * @version $Id: CliCommandHelper.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class CliCommandHelper {
    
    private static final Logger log = Logger.getLogger(CliCommandHelper.class);

    public static void searchAndRun(String[] args, String basePackage) {
        List<CliCommand> commandList = new ArrayList<CliCommand>();
        List<String> mainCommands = new ArrayList<String>();
        ServiceLoader<? extends CliCommandPlugin> serviceLoader = ServiceLoader.load(CliCommandPlugin.class);
            
        // Extract all the commands from the plugins
        for (CliCommandPlugin cliCommandPlugin : serviceLoader) {
            try {
                final String mainCommand = cliCommandPlugin.getMainCommand();
                final String subCommand = cliCommandPlugin.getSubCommand();
                final String description = cliCommandPlugin.getDescription();
                final String[] commmandAliases = cliCommandPlugin.getMainCommandAliases();
                final String[] subcommandAliases = cliCommandPlugin.getSubCommandAliases();
                if (subCommand == null || subCommand.trim().length() == 0
                        || description == null || description.trim().length() == 0) {
                    log.warn("Will not register plugin class " + mainCommand + ": Required getter returned an empty String.");
                    continue;
                }
                // log.debug(" main: " + mainCommand + " sub: " + subCommand + " description: " + description);
                commandList.add(new CliCommand(mainCommand, commmandAliases, subCommand, subcommandAliases, description, cliCommandPlugin));
                if (!mainCommands.contains(mainCommand)) {
                    mainCommands.add(mainCommand);
                }
            } catch (Exception e) {
                log.warn("Will not register plugin class " + cliCommandPlugin.getMainCommand() + ": " + e.getMessage());
                log.debug("Will not register plugin class " + cliCommandPlugin.getMainCommand() + ": ", e);
                continue;
            }
        }
        // Look for (and execute if found) commands that don't have main command
        List<CliCommand> subTargetsOnly = new ArrayList<CliCommand>();
        for (CliCommand cliCommand : commandList) {
            if (cliCommand.getMainCommand() == null) {
                if (args.length > 0 && cliCommand.getSubCommand().equalsIgnoreCase(args[0])) {
                    executeCommand(cliCommand.getCommand(), args, false);
                    return;
                }
                subTargetsOnly.add(cliCommand);
            }
        }
        // Look for all sub commands (and execute if found)
        List<CliCommand> subTargets = new ArrayList<CliCommand>();
        for (CliCommand cliCommand : commandList) {
            //Check for the main command
            if (args.length > 0 && cliCommand.getMainCommand() != null) {
                boolean isMainCommand = cliCommand.getMainCommand().equalsIgnoreCase(args[0]);
                //Check if one of the aliases was used.
                boolean isAliasCommand = false;
                for (String alias : cliCommand.getCommandAliases()) {
                    if (alias.equalsIgnoreCase(args[0])) {
                        isAliasCommand = true;                               
                        break;
                    }
                }
                if (isMainCommand || isAliasCommand) {
                    if (args.length > 1) {
                        boolean isSubCommand = cliCommand.getSubCommand().equalsIgnoreCase(args[1]);
                        boolean isSubCommandAlias = false;
                        for(String subCommandAlias : cliCommand.getSubCommandAliases()) {
                            if(subCommandAlias.equalsIgnoreCase(args[1])) {
                                isSubCommandAlias = true;
                            }
                        }
                        if (isSubCommand || isSubCommandAlias) {
                            if (isAliasCommand) {
                                log.error("WARNING: The command <" + args[0] + "> is deprecated and will soon be removed."
                                        + " Please change to using" + " the command <" + cliCommand.getMainCommand() + "> instead.");
                            }
                            if (isSubCommandAlias) {
                                log.error("WARNING: The subcommand <" + args[1] + "> is deprecated and will soon be removed."
                                        + " Please change to using" + " the command <" + cliCommand.getSubCommand() + "> instead.");
                            }
                            executeCommand(cliCommand.getCommand(), args, true);
                            return;
                        }
                    }
                    subTargets.add(cliCommand);
                }
            }
        }
        // If we didn't execute something by now the command wasn't found
        if (subTargets.isEmpty()) { 
            String mainCommandsString = "";
            for (String mainCommand : mainCommands) {
                mainCommandsString += (mainCommandsString.length() == 0 ? "" : " | ") + (mainCommand != null ? mainCommand : "");
            }
            if (mainCommandsString.length()>0) {
                log.info("Missing or invalid argument. Use one of [" + mainCommandsString + "] to see additional sub commands.");
                if (!subTargetsOnly.isEmpty()) {
                    log.info("Or use one of:");
                }
            }
            if (!subTargetsOnly.isEmpty()) {
                showSubCommands(subTargetsOnly);
            }
            return;
        }
        log.info("Available sub commands for '" + args[0] + "':");
        showSubCommands(subTargets);
    }

    private static void showSubCommands(List<CliCommand> list) {
        Collections.sort(list);
        for (CliCommand cliCommand : list) {
            log.info(String.format("  %-20s %s", cliCommand.getSubCommand(), cliCommand.getDescription()));
        }
    }

    /**
     * 
     */
    private static void executeCommand(CliCommandPlugin command, String[] args, boolean shiftArgs) {
        log.debug("Executing " + command.getMainCommand());
        try {
            command.execute(shiftArgs ? shiftStringArray(args) : args);
            return;
        } catch (Exception e) {
            log.error("Could not run execute method for class " + command.getMainCommand(), e);
            System.exit(1);
        }
    }

    /**
     * Remove the first entry in the String array
     */
    private static String[] shiftStringArray(String[] input) {
        String[] output = new String[input.length - 1];
        System.arraycopy(input, 1, output, 0, input.length-1);
        return output;
    }
}
