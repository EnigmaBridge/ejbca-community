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
package org.ejbca.ui.cli.config.scep;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;

/**
 * @version $Id: ListAliasCommand.java 18666 2014-03-24 13:37:16Z mikekushner $
 *
 */
public class ListAliasCommand extends BaseScepConfigCommand {

    private static final Logger log = Logger.getLogger(ListAliasCommand.class);

    @Override
    public String getMainCommand() {
        return "listalias";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        Set<String> aliaslist = getScepConfiguration().getAliasList();
        Iterator<String> itr = aliaslist.iterator();
        while (itr.hasNext()) {
            log.info(itr.next());
        }
        return CommandResult.SUCCESS;

    }

    @Override
    public String getCommandDescription() {
        return "Lists all existing SCEP configuration aliases";
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
