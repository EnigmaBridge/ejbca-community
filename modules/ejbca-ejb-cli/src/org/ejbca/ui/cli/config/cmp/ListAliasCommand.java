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

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;

/**
 * @version $Id: ListAliasCommand.java 19902 2014-09-30 14:32:24Z anatom $
 *
 */
public class ListAliasCommand extends BaseCmpConfigCommand {

    private static final Logger log = Logger.getLogger(ListAliasCommand.class);

    @Override
    public String getMainCommand() {
        return "listalias";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        Set<String> aliaslist = getCmpConfiguration().getAliasList();
        Iterator<String> itr = aliaslist.iterator();
        while (itr.hasNext()) {
            log.info(itr.next());
        }
        return CommandResult.SUCCESS;

    }

    @Override
    public String getCommandDescription() {
        return "Lists all existing CMP configuration aliases";
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
