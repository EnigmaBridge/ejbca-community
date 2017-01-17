package org.ejbca.core.ejb.vpn;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.util.EjbRemoteHelper;

/**
 * Base helper for classes requiring auth token
 *
 * @author ph4r05
 * Created by dusanklinec on 17.01.17.
 */
public abstract class VpnBaseHelper {

    protected AuthenticationToken authenticationToken;
    protected AuthenticationTokenProvider authenticationTokenProvider;
    protected boolean fetchRemoteSessions = true;

    /**
     * Returns auth token. If provider is registered, provider is used.
     * Otherwise static token is returned.
     * @return auth token
     */
    protected AuthenticationToken getAuthToken(){
        if (authenticationTokenProvider != null){
            return authenticationTokenProvider.getAuthenticationToken();
        }

        return authenticationToken;
    }

    /**
     * Returns a cached remote session bean.
     *
     * @param key the @Remote-appended interface for this session bean
     * @return the sought interface, or null if it doesn't exist in JNDI context.
     */
    public static <T> T getRemoteSession(final Class<T> key) {
        return EjbRemoteHelper.INSTANCE.getRemoteSession(key);
    }

    public AuthenticationToken getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(AuthenticationToken authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public AuthenticationTokenProvider getAuthenticationTokenProvider() {
        return authenticationTokenProvider;
    }

    public void setAuthenticationTokenProvider(AuthenticationTokenProvider authenticationTokenProvider) {
        this.authenticationTokenProvider = authenticationTokenProvider;
    }
}
