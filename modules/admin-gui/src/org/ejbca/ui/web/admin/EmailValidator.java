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

package org.ejbca.ui.web.admin;

import org.apache.log4j.Logger;
import org.cesecore.util.StringTools;
import org.ejbca.core.ejb.vpn.VpnUtils;
import org.ejbca.ui.web.admin.configuration.EjbcaJSFHelper;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * JSF validator to check that input fields for valid email.
 */
public class EmailValidator implements Validator {
	private static final Logger log = Logger.getLogger(EmailValidator.class);

	public void validate(FacesContext facesContext, UIComponent uIComponent, Object object) throws ValidatorException {
		final String textFieldValue = (String)object;
		if (log.isDebugEnabled()) {
			log.debug("Validating component " + uIComponent.getClientId(facesContext) + " with value \"" + textFieldValue + "\"");
		}

		if (!VpnUtils.isEmailValid(textFieldValue)) {
		    final String msg = EjbcaJSFHelper.getBean().getEjbcaWebBean().getText("INVALIDEMAIL");
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
		}
	}
}
