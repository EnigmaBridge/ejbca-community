package org.ejbca.cesecoreintegration;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.cesecore.time.TrustedTime;
import org.cesecore.time.TrustedTimeWatcherSessionLocal;
import org.cesecore.time.providers.TrustedTimeProviderException;

/**
 * This is the trusted time watcher implementation.
 * 
 *
 * @version $Id: ServerTimeWatcherSessionBean.java 12823 2011-10-05 09:46:53Z mikekushner $
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ServerTimeWatcherSessionBean implements TrustedTimeWatcherSessionLocal {

	@Override
	public TrustedTime getTrustedTime(final boolean force) throws TrustedTimeProviderException {
		final TrustedTime tt = new TrustedTime();
		tt.setSync(false);
		return tt;
	}


}
