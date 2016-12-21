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
package org.ejbca.core.ejb.ca.publisher;

import java.security.cert.Certificate;
import java.util.Collection;

import javax.ejb.Remote;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.publisher.BasePublisher;
import org.ejbca.core.model.ca.publisher.PublisherConnectionException;
import org.ejbca.core.model.ca.publisher.PublisherDoesntExistsException;
import org.ejbca.core.model.ca.publisher.PublisherExistsException;

/**
 * @version $Id: PublisherProxySessionRemote.java 21141 2015-04-27 11:27:26Z mikekushner $
 *
 */
@Remote
public interface PublisherProxySessionRemote {

    /**
     * Adds a publisher to the database.
     * @throws PublisherExistsException if hard token already exists.
     * @throws AuthorizationDeniedException 
     */
    int addPublisher(AuthenticationToken admin, String name, BasePublisher publisher) throws PublisherExistsException, AuthorizationDeniedException;
     
    /**
     * Adds a publisher with the same content as the original.
     * @throws PublisherExistsException 
     * @throws AuthorizationDeniedException 
     * @throws PublisherDoesntExistsException 
     * @throws PublisherExistsException if publisher already exists.
     */
    void clonePublisher(AuthenticationToken admin, String oldname, String newname) throws PublisherDoesntExistsException, AuthorizationDeniedException, PublisherExistsException;
    
    /**
     * Returns a publisher id, given it's publishers name
     * @return the id or 0 if the publisher cannot be found.
     */
    int getPublisherId(String name);

    /**
     * Returns a publishers name given its id.
     * @return the name or null if id does not exist
     */
    String getPublisherName(int id);

    /** Removes a publisher from the database. 
     * @throws AuthorizationDeniedException */
    void removePublisher(AuthenticationToken admin, String name) throws AuthorizationDeniedException;
    
    /**
     * Renames a publisher.
     * @throws PublisherExistsException if publisher already exists.
     * @throws AuthorizationDeniedException 
     */
    void renamePublisher(AuthenticationToken admin, String oldname, String newname) throws PublisherExistsException, AuthorizationDeniedException;
    
    /**
     * Revokes the certificate in the given collection of publishers. See
     * BasePublisher class for further documentation about function
     * 
     * @param publisherids
     *            a Collection (Integer) of publisher IDs.
     * @throws AuthorizationDeniedException 
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    void revokeCertificate(AuthenticationToken admin, Collection<Integer> publisherids, Certificate cert,
            String username, String userDN, String cafp, int type, int reason, long revocationDate, String tag,
            int certificateProfileId, long lastUpdate) throws AuthorizationDeniedException;
    
    
    /**
     * Test the connection to of a publisher
     * 
     * @param publisherid
     *            the id of the publisher to test.
     * @throws PublisherConnectionException if connection test with publisher fails.
     * @see org.ejbca.core.model.ca.publisher.BasePublisher
     */
    void testConnection(int publisherid) throws PublisherConnectionException;
    
    /**
     * Makes sure that no Publishers are cached to ensure that we read from database
     * next time we try to access it.
     */
    void flushPublisherCache();

    /** Change a Publisher without affecting the cache */
    void internalChangeCertificateProfileNoFlushCache(String name, BasePublisher publisher)
            throws AuthorizationDeniedException; 
    
    int adhocUpgradeTo6_3_1_1();
    

}
