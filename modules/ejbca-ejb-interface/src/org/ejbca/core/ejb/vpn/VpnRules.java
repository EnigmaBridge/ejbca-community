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

/**
 * VPN related access rules.
 *
 * @author ph4r05
 */
public enum VpnRules {
    BASE("/vpn"),
    USER(BASE.resource() + "/user"),

    MODIFY(BASE.resource() + "/modify"),
    DELETE(BASE.resource() + "/delete"),
    VIEW(BASE.resource() + "/view"),
    USE(BASE.resource() + "/use"),
    CRL_GET(BASE.resource() + "/crl/get"),
    CRL_GEN(BASE.resource() + "/crl/gen"),

    USER_VIEW(USER.resource() + "/view"),
    USER_NEW(USER.resource() + "/new"),
    USER_DELETE(USER.resource() + "/delete"),
    USER_REVOKE(USER.resource() + "/revoke"),
    USER_MODIFY(USER.resource() + "/modify"),
    USER_GENERATE(USER.resource() + "/generate"),
    USER_MAIL(USER.resource() + "/mail"),
    USER_LINK(USER.resource() + "/link"),
    ;

    private final String resource;

    private VpnRules(String resource) {
        this.resource = resource;
    }

    public String resource() {
        return this.resource;
    }

    public String toString() {
        return this.resource;
    }
}
