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

package org.ejbca.ui.cli.ra;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionRemote;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ra.raadmin.UserDoesntFullfillEndEntityProfile;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

/**
 * Set the Subject Directory Attributes for an end entity.
 *
 * @version $Id: SetSubjDirAttrCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public class SetSubjDirAttrCommand extends BaseRaCommand {

    private static final Logger log = Logger.getLogger(SetSubjDirAttrCommand.class);

    private static final String USERNAME_KEY = "--username";
    private static final String ATTRIBUTES_KEY = "--attr";

    {
        registerParameter(new Parameter(USERNAME_KEY, "Username", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Username of the end entity to modify."));
        registerParameter(new Parameter(ATTRIBUTES_KEY, "Attributes", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Attributes are: dateOfBirth=<19590927>, placeOfBirth=<string>, gender=<M/F>, countryOfCitizenship=<two letter ISO3166>, countryOfResidence=<two letter ISO3166>"));
    }

    @Override
    public String getMainCommand() {
        return "setsubjectdirattr";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        String username = parameters.get(USERNAME_KEY);
        String attributes = parameters.get(ATTRIBUTES_KEY);
        if (StringUtils.isEmpty(attributes)) {
            getLogger().error("Subject directory attributes must be supplied.");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        getLogger().info("Setting subject directory attributes '" + attributes + "' for end entity " + username);
        try {
            EndEntityInformation uservo = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityAccessSessionRemote.class).findUser(
                    getAuthenticationToken(), username);
            ExtendedInformation ext = uservo.getExtendedinformation();
            if (ext == null) {
                ext = new ExtendedInformation();
            }
            ext.setSubjectDirectoryAttributes(attributes);
            uservo.setExtendedinformation(ext);
            EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).changeUser(getAuthenticationToken(), uservo, false);
            return CommandResult.SUCCESS;
        } catch (AuthorizationDeniedException e) {
            getLogger().error("Not authorized to change end entity.");
        } catch (UserDoesntFullfillEndEntityProfile e) {
            getLogger().error("Given end entity doesn't fullfill end entity profile. : " + e.getMessage());
        } catch (CADoesntExistsException e) {
            getLogger().error("ERROR: " + e.getMessage());
        } catch (WaitingForApprovalException e) {
            getLogger().error("ERROR: " + e.getMessage());
        } catch (EjbcaException e) {
            getLogger().error("ERROR: " + e.getMessage());
        }
        return CommandResult.FUNCTIONAL_FAILURE;
    }

    @Override
    public String getCommandDescription() {
        return "Set the Subject Directory Attributes for a end entity";
    }

    @Override
    public String getFullHelpText() {
        return getCommandDescription();
    }

    protected Logger getLogger() {
        return log;
    }

}
