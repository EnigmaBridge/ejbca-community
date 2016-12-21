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
package org.ejbca.core.model.services;

import java.util.Map;
import java.util.Properties;

/**
 * Interface used to define a service action. An action could be to generate
 * a email, report, write to file.
 * 
 * Its main method is perormAction() that should do the work.
 * 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: IAction.java 19901 2014-09-30 14:29:38Z anatom $
 */
public interface IAction {
	
	/**
	 * Method that should initialize the action according to the 
	 * configured properties.
	 * 
	 * This method should be called before the any action calls are made.
	 */
	public void init(Properties properties, String serviceName);
	
	
	/**
	 * The main method used to signal that it's time to perform an action according
	 * to the data sent in the parameter IActionInfo
	 * 
	 * @param optional parameter used to send data to the action
	 * @param ejbs A map between Local EJB interface classes and their injected stub
	 * @throws ActionException if the action failed in any way.
	 */
	public void performAction(ActionInfo actionInfo, Map<Class<?>, Object> ejbs) throws ActionException;

}
