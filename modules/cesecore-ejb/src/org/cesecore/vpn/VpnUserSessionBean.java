/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.vpn;

import org.apache.log4j.Logger;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.QueryResultWrapper;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Basic CRUD and activation caching of VpnUsers is provided through this local access SSB.
 * 
 * @version $Id: VpnUserSessionBean.java 19678 2014-09-03 10:06:54Z aveen4711 $
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "VpnUserSessionw")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class VpnUserSessionBean implements VpnUserSession {

    private static final Logger log = Logger.getLogger(VpnUserSessionBean.class);
    private static final InternalResources intres = InternalResources.getInstance();

    @PersistenceContext(unitName = CesecoreConfiguration.PERSISTENCE_UNIT)
    private EntityManager entityManager;

    @PostConstruct
    public void postConstruct() {
        CryptoProviderTools.installBCProviderIfNotAvailable();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public void flushCache() {
//        VpnUserCache.INSTANCE.flush();
        if (log.isDebugEnabled()) {
            log.debug("Flushed VpnUser cache.");
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public void flushExcludingIDs(List<String> ids) {
//        VpnUserCache.INSTANCE.replaceCacheWith(ids);
        if (log.isDebugEnabled()) {
            log.debug("Flushed VpnUser cache except for " + ids.size() + " specific entries.");
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public VpnUser getVpnUser(final String vpnUserId) {
        if (log.isDebugEnabled()) {
            log.debug("VpnUser with ID " + vpnUserId + " will be checked for updates.");
        }

        final VpnUser vpnUser = readVpnUser(vpnUserId);
        return vpnUser;
    }

    @Override
    public VpnUser mergeVpnUser(final VpnUser vpnUser) throws VpnUserNameInUseException {
        if (log.isTraceEnabled()) {
            log.trace(">addVpnUser " + vpnUser.getUsername() + " " + vpnUser.getClass().getName());
        }

        final String vpnUserId = vpnUser.getUsername();
        final long lastUpdate = System.currentTimeMillis();

        VpnUser vpnUserObj = entityManager.find(VpnUser.class, vpnUserId);
        if (vpnUserObj == null) {
            // The vpnUser does not exist in the database, before we add it we want to check that the name is not in use
            if (isVpnUserNameUsed(vpnUserId)) {
                throw new VpnUserNameInUseException(intres.getLocalizedMessage("token.nameisinuse", vpnUserId));
            }

            vpnUserObj = new VpnUser(vpnUserId);
            vpnUserObj.setDateCreated(lastUpdate);
            vpnUserObj.setDateModified(lastUpdate);
            vpnUserObj.setRevokedStatus(0);
        } else {
            // It might be the case that the calling transaction has already loaded a reference to this token
            // and hence we need to get the same one and perform updates on this object instead of trying to
            // merge a new object.
            vpnUserObj.setDateModified(lastUpdate);
        }

        vpnUserObj = createOrUpdateVpnUser(vpnUserObj);

        // Update cache with provided token (it might be active and we like keeping things active)
//        VpnUserCache.INSTANCE.updateWith(vpnUserId, vpnUserObj.getProtectString(0).hashCode(), tokenName, vpnUser);
        if (log.isTraceEnabled()) {
            log.trace("<addVpnUser " + vpnUser.getUsername());
        }

        return vpnUserObj;   // tokenId
    }

    @Override
    public boolean removeVpnUser(final String vpnUserId) {
        final boolean ret = deleteVpnUser(vpnUserId);
//        VpnUserCache.INSTANCE.updateWith(cryptoTokenId, 0, null, null);
        return ret;
    }
    
//    @Override
//    public Map<String,Integer> getCachedNameToIdMap() {
//        return VpnUserCache.INSTANCE.getNameToIdMap();
//    }
    
    @Override
    public boolean isVpnUserNameUsed(final String userName) {
        final Query query = entityManager.createQuery("SELECT a FROM VpnUser a WHERE a.username=:userName");
        query.setParameter("userName", userName);
        return !query.getResultList().isEmpty();
    }

//    @Override
//    public boolean isVpnUserNameUsedByIdOnly(final String cryptoTokenName, final int cryptoTokenId) {
//        final Query query = entityManager.createQuery("SELECT a FROM VpnUserData a WHERE a.tokenName=:tokenName");
//        query.setParameter("tokenName", cryptoTokenName);
//        @SuppressWarnings("unchecked")
//        final List<VpnUserData> cryptoTokenDatas = query.getResultList();
//        for (final VpnUserData cryptoTokenData: cryptoTokenDatas) {
//            if (cryptoTokenData.getId() != cryptoTokenId) {
//                return false;
//            }
//        }
//        return true;
//    }

    //
    // Create Read Update Delete (CRUD) methods
    //

    private VpnUser readVpnUser(final String vpnUserId) {
        final Query query = entityManager.createQuery("SELECT a FROM VpnUser a WHERE a.username=:id");
        query.setParameter("id", vpnUserId);
        return QueryResultWrapper.getSingleResult(query);
    }

    private VpnUser createOrUpdateVpnUser(final VpnUser data) {
        return entityManager.merge(data);
    }

    private boolean deleteVpnUser(final String vpnUserId) {
        final Query query = entityManager.createQuery("DELETE FROM VpnUser a WHERE a.username=:id");
        query.setParameter("id", vpnUserId);
        return query.executeUpdate() == 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getVpnUserIds() {
        return entityManager.createQuery("SELECT a.username FROM VpnUser a").getResultList();
    }
}
