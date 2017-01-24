package org.ejbca.core.ejb.vpn;

import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.vpn.VpnUser;
import org.ejbca.core.model.SecConst;

import java.util.Date;

/**
 * User create helper
 *
 * @author ph4r05
 * Created by dusanklinec on 23.01.17.
 */
public class VpnUserHelper {

    /**
     * Initializes new end entity object from the VpnUser template.
     * Used to create a new user.
     *
     * If auto-generated password is used in end entity profile, entity password has to be null.
     * Password will be generated automatically and sent via email to the end entity.
     *
     * @param tplUser template VPN user
     * @param caId CA ID
     * @param entityProfileId End entity profile ID
     * @param certProfileId Certificate profile ID
     * @return new end entity user information
     */
    public static EndEntityInformation newEndEntity(VpnUser tplUser, int caId, int entityProfileId, int certProfileId){
        final EndEntityInformation userEi = new EndEntityInformation(
                VpnUtils.getUserName(tplUser),
                VpnUtils.genUserCN(tplUser),
                caId,
                VpnUtils.genUserAltName(tplUser),
                tplUser.getEmail(),
                EndEntityConstants.STATUS_NEW,
                EndEntityTypes.ENDUSER.toEndEntityType(),
                entityProfileId,
                certProfileId,
                new Date(), new Date(),
                SecConst.TOKEN_SOFT_P12,
                0, null);

        // If auto-generated password is used in end entity profile, this password has to be null.
        // Password will be generated automatically and sent via email to the end entity.
        userEi.setPassword(VpnUtils.genRandomPwd());
        userEi.setCardNumber(null);
        return userEi;
    }

}
