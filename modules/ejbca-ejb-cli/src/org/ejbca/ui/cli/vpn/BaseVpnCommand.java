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
 
package org.ejbca.ui.cli.vpn;

import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.ejb.vpn.VpnCons;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.ui.cli.infrastructure.command.EjbcaCliUserCommandBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base for VPN commands, contains common functions for VPN operation.
 *
 * @version $Id: BaseRaCommand.java 19902 2014-09-30 14:32:24Z anatom $
 */
public abstract class BaseVpnCommand extends EjbcaCliUserCommandBase {

	public static final String MAINCOMMAND = "vpn";
	
    @Override
    public String[] getCommandPath() {
        return new String[] { MAINCOMMAND };
    }

    /**
     * VPN end entity client profile
     * @return
     * @throws EndEntityProfileNotFoundException
     */
    protected int getVpnClientEndEntityProfile() throws EndEntityProfileNotFoundException {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class)
                .getEndEntityProfileId(VpnCons.DEFAULT_END_ENTITY_PROFILE); // TODO: to config
    }

    /**
     * Returns VPN CA.
     * @return
     * @throws AuthorizationDeniedException
     * @throws CADoesntExistsException
     */
    protected CAInfo getVpnCA() throws AuthorizationDeniedException, CADoesntExistsException {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                .getCAInfo(getAuthenticationToken(), VpnCons.DEFAULT_CA); // TODO: to config
    }

    /**
     * Returns list of all CAs.
     * @return
     * @throws AuthorizationDeniedException
     */
    protected List<CAInfo> getCAs() throws AuthorizationDeniedException {
        final Collection<Integer> cas = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                .getAuthorizedCaIds(getAuthenticationToken());
        final List<CAInfo> infoList = new ArrayList<>(cas.size());

        try {
            for (int caid : cas) {
                CAInfo info = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class)
                        .getCAInfo(getAuthenticationToken(), caid);
                infoList.add(info);
            }
        } catch (CADoesntExistsException e) {
            throw new IllegalStateException("CA couldn't be retrieved even though it was just referenced.");
        }

        return infoList;
    }
}
