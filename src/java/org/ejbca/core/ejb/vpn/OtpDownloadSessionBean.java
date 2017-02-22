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
package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.cesecore.config.CesecoreConfiguration;
import org.cesecore.internal.InternalResources;
import org.cesecore.jndi.JndiConstants;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.QueryResultWrapper;
import org.cesecore.vpn.OtpDownload;
import org.cesecore.vpn.VpnUser;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Basic CRUD and activation caching of VpnUsers is provided through this local access SSB.
 *
 * @author ph4r05
 */
@Stateless(mappedName = JndiConstants.APP_JNDI_PREFIX + "OtpDownloadSession")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OtpDownloadSessionBean implements OtpDownloadSession {

    private static final Logger log = Logger.getLogger(OtpDownloadSessionBean.class);
    private static final InternalResources intres = InternalResources.getInstance();

    @PersistenceContext(unitName = CesecoreConfiguration.PERSISTENCE_UNIT)
    private EntityManager entityManager;

    @PostConstruct
    public void postConstruct() {
        CryptoProviderTools.installBCProviderIfNotAvailable();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public OtpDownload getOtp(final int otpId) {
        return readOtpDownload(otpId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public List<OtpDownload> getOtp(final String otpId) {
        return readOtpDownload(otpId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public List<OtpDownload> getOtp(final String otpType, final String otpId) {
        return readOtpDownload(otpType, otpId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public List<OtpDownload> getOtp(final String otpType, final String otpId, final String resource) {
        return readOtpDownload(otpType, otpId, resource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getIds() {
        return entityManager.createQuery("SELECT a.id FROM OtpDownload a").getResultList();
    }

    @Override
    public OtpDownload merge(final OtpDownload otpDownload) {
        final long lastUpdate = System.currentTimeMillis();

        Integer otpDownloadId = otpDownload.getId();
        OtpDownload otpObj = entityManager.find(OtpDownload.class, otpDownloadId);
        if (otpObj == null) {
            // Simple clone
            otpObj = OtpDownload.copy(otpDownload);

        } else {
            // It might be the case that the calling transaction has already loaded a reference to this otp download
            // and hence we need to get the same one and perform updates on this object instead of trying to
            // merge a new object.
            otpObj.setOtpResource(otpDownload.getOtpResource());
            otpObj.setDateModified(lastUpdate);
            otpObj.setOtpDownload(otpDownload.getOtpDownload());
            otpObj.setOtpUsed(otpDownload.getOtpUsed());
            otpObj.setOtpFirstUsed(otpDownload.getOtpFirstUsed());
            otpObj.setOtpUsedDescriptor(otpDownload.getOtpUsedDescriptor());
            otpObj.setOtpCookie(otpDownload.getOtpCookie());
            otpObj.setOtpUsedCount(otpDownload.getOtpUsedCount());
            otpObj.setAuxData(otpDownload.getAuxData());
        }

        otpObj = createOrUpdate(otpObj);
        return otpObj;
    }

    @Override
    public boolean remove(final int otpId) {
        final boolean ret = delete(otpId);
        return ret;
    }

    @Override
    public boolean remove(final String otpType, final String otpId, final String otpResource) {
        final boolean ret = delete(otpType, otpId, otpResource);
        return ret;
    }

    @Override
    public OtpDownload downloadOtp(final String otpToken) {
        final TypedQuery<OtpDownload> query = entityManager.createQuery(
                "SELECT a FROM OtpDownload a WHERE a.otpDownload=:otp", OtpDownload.class);
        query.setParameter("otp", otpToken);
        return QueryResultWrapper.getSingleResult(query);
    }

    @Override
    public boolean isOtpTokenTaken(final String otpType, final String otpId, final String otpResource) {
        final Query query = entityManager.createQuery("SELECT a FROM OtpDownload a WHERE " +
                "a.otpType=:otpType AND a.otpId=:otpId AND a.otpResource=:otpResource");
        query.setParameter("otpType", otpType);
        query.setParameter("otpId", otpId);
        query.setParameter("otpResource", otpResource);
        return !query.getResultList().isEmpty();
    }

    //
    // Create Read Update Delete (CRUD) methods
    //

    private OtpDownload readOtpDownload(final int otpId) {
        final Query query = entityManager.createQuery("SELECT a FROM OtpDownload a WHERE a.id=:id");
        query.setParameter("id", otpId);
        return QueryResultWrapper.getSingleResult(query);
    }

    private List<OtpDownload> readOtpDownload(final String otpId) {
        final TypedQuery<OtpDownload> query = entityManager.createQuery(
                "SELECT a FROM OtpDownload a WHERE a.otpId=:otpId", OtpDownload.class);
        query.setParameter("otpId", otpId);
        return query.getResultList();
    }

    private List<OtpDownload> readOtpDownload(final String otpType, final String otpId) {
        final TypedQuery<OtpDownload>  query = entityManager.createQuery("SELECT a FROM OtpDownload a " +
                " WHERE a.otpType=:otpType AND a.otpId=:otpId", OtpDownload.class);
        query.setParameter("otpType", otpType);
        query.setParameter("otpId", otpId);
        return query.getResultList();
    }

    private List<OtpDownload> readOtpDownload(final String otpType, final String otpId, final String otpResource) {
        final TypedQuery<OtpDownload>  query = entityManager.createQuery("SELECT a FROM OtpDownload a " +
                " WHERE a.otpType=:otpType AND a.otpId=:otpId and a.otpResource=:otpResource", OtpDownload.class);
        query.setParameter("otpType", otpType);
        query.setParameter("otpId", otpId);
        query.setParameter("otpResource", otpResource);
        return query.getResultList();
    }

    private OtpDownload createOrUpdate(final OtpDownload data) {
        return entityManager.merge(data);
    }

    private boolean delete(final int otpId) {
        final Query query = entityManager.createQuery("DELETE FROM OtpDownload a WHERE a.id=:id");
        query.setParameter("id", otpId);
        return query.executeUpdate() == 1;
    }

    private boolean delete(final String otpType, final String otpId, final String otpResource) {
        final TypedQuery<OtpDownload>  query = entityManager.createQuery("DELETE FROM OtpDownload a " +
                " WHERE a.otpType=:otpType AND a.otpId=:otpId and a.otpResource=:otpResource", OtpDownload.class);
        query.setParameter("otpType", otpType);
        query.setParameter("otpId", otpId);
        query.setParameter("otpResource", otpResource);
        return query.executeUpdate() == 1;
    }
}
