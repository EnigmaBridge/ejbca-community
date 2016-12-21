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
package org.ejbca.ui.cli.config.cmp;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

/**
 * @version $Id: AddAliasCommand.java 19974 2014-10-09 15:19:06Z mikekushner $
 *
 */
public class AddAliasCommand extends BaseCmpConfigCommand {

    private static final String ALIAS_KEY = "--alias";

    private static final Logger log = Logger.getLogger(AddAliasCommand.class);

    {
        registerParameter(new Parameter(ALIAS_KEY, "Alias", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "The alias to add."));
    }

    @Override
    public String getMainCommand() {
        return "addalias";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        String alias = parameters.get(ALIAS_KEY);
        // We check first because it is unnecessary to call saveConfiguration when it is not needed
        if (getCmpConfiguration().aliasExists(alias)) {
            log.info("Alias '" + alias + "' already exists.");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        getCmpConfiguration().addAlias(alias);
        try {
            getGlobalConfigurationSession().saveConfiguration(getAuthenticationToken(), getCmpConfiguration());
            log.info("Added CMP alias: " + alias);
            getGlobalConfigurationSession().flushConfigurationCache(CmpConfiguration.CMP_CONFIGURATION_ID);
            return CommandResult.SUCCESS;
        } catch (AuthorizationDeniedException e) {
            log.info("Failed to add alias '" + alias + "': " + e.getLocalizedMessage());
            return CommandResult.AUTHORIZATION_FAILURE;
        }

    }

    @Override
    public String getCommandDescription() {
        return "Adds a CMP configuration alias.";
    }

    @Override
    public String getFullHelpText() {
        return getCommandDescription();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
