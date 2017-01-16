package org.ejbca.core.ejb.vpn;

import org.cesecore.authentication.tokens.AuthenticationToken;

/**
 * Auth token provider.
 * Auth token is valid for only one remote call. We need more remote calls
 * thus this provider is used.
 *
 * @author ph4r05
 */
public interface AuthenticationTokenProvider {
    AuthenticationToken getAuthenticationToken();
}
