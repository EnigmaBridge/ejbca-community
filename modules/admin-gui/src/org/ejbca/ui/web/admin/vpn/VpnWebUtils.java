package org.ejbca.ui.web.admin.vpn;

import org.ejbca.ui.web.admin.cainterface.CAInterfaceBean;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;
import org.ejbca.ui.web.admin.rainterface.RAInterfaceBean;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.beans.Beans;
import java.io.IOException;

/**
 * Simple utilities for VPN beans.
 *
 * @author ph4r05
 * Created by dusanklinec on 05.01.17.
 */
public class VpnWebUtils {

    /**
     * Gets RAInterface bean in the Faces context.
     * @return RAInterfaceBean
     */
    public static RAInterfaceBean getRaBean() throws IOException, ClassNotFoundException {
        RAInterfaceBean raif = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) ctx.getExternalContext().getSession(true);
        synchronized (session){
            raif = (org.ejbca.ui.web.admin.rainterface.RAInterfaceBean) session.getAttribute("rabean");
            if (raif == null){
                raif = (org.ejbca.ui.web.admin.rainterface.RAInterfaceBean) java.beans.Beans.instantiate(
                        Thread.currentThread().getContextClassLoader(),
                        org.ejbca.ui.web.admin.rainterface.RAInterfaceBean.class.getName());
                session.setAttribute("rabean", raif);
            }
        }

        return raif;
    }

    /**
     * Gets RAInterfaceBean in the servlet context
     */
    public static RAInterfaceBean getRaBean(HttpServletRequest req)
            throws ServletException
    {
        return getRaBean(req, null);
    }

    /**
     * Gets RAInterfaceBean in the servlet context with web bean initialization.
     * If webBean is null, {@link VpnWebUtils#getEjbcaWebBean(HttpServletRequest)} call is used to init the RA bean.
     */
    public static RAInterfaceBean getRaBean(HttpServletRequest req, EjbcaWebBean webBean)
            throws ServletException
    {
        HttpSession session = req.getSession();
        RAInterfaceBean rabean = (RAInterfaceBean) session.getAttribute("rabean");
        if (rabean == null) {
            try {
                rabean = (RAInterfaceBean) Beans.instantiate(Thread.currentThread().getContextClassLoader(), org.ejbca.ui.web.admin.rainterface.RAInterfaceBean.class.getName());
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (Exception e) {
                throw new ServletException("Unable to instantiate RAInterfaceBean", e);
            }
            try {
                rabean.initialize(req, webBean != null ? webBean : getEjbcaWebBean(req));
            } catch (Exception e) {
                throw new ServletException("Cannot initialize RAInterfaceBean", e);
            }
            session.setAttribute("rabean", rabean);
        }
        return rabean;
    }

    /**
     * Gets EJBCA web bean in servlet context.
     */
    public static  EjbcaWebBean getEjbcaWebBean(HttpServletRequest req)
            throws ServletException
    {
        return getEjbcaWebBean(req, (String[]) null);
    }

    /**
     * Gets EJBCA web bean in servlet context, with resource authorization.
     */
    public static  EjbcaWebBean getEjbcaWebBean(HttpServletRequest req, String... resources)
            throws ServletException
    {
        HttpSession session = req.getSession();
        EjbcaWebBean ejbcawebbean= (EjbcaWebBean)session.getAttribute("ejbcawebbean");
        if ( ejbcawebbean == null ){
            try {
                ejbcawebbean = (EjbcaWebBean) java.beans.Beans.instantiate(Thread.currentThread().getContextClassLoader(), org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName());
            } catch (ClassNotFoundException exc) {
                throw new ServletException(exc.getMessage());
            }catch (Exception exc) {
                throw new ServletException (" Cannot create bean of class "+org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName(), exc);
            }

            if (resources != null) {
                try {
                    ejbcawebbean.initialize(req, resources);
                } catch (Exception e) {
                    throw new ServletException("Authorization Denied");
                }
            }

            session.setAttribute("ejbcawebbean", ejbcawebbean);
        }
        return ejbcawebbean;
    }

    /**
     * Gets CA web bean in servlet context.
     */
    public static  CAInterfaceBean getCaBean(HttpServletRequest req)
            throws ServletException
    {
        HttpSession session = req.getSession();
        CAInterfaceBean cabean = (CAInterfaceBean) session.getAttribute("cabean");
        if (cabean == null) {
            try {
                cabean = (CAInterfaceBean) Beans.instantiate(Thread.currentThread().getContextClassLoader(), org.ejbca.ui.web.admin.cainterface.CAInterfaceBean.class.getName());
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (Exception e) {
                throw new ServletException("Unable to instantiate CAInterfaceBean", e);
            }
            try {
                cabean.initialize(getEjbcaWebBean(req));
            } catch (Exception e) {
                throw new ServletException("Cannot initialize CAInterfaceBean", e);
            }
            session.setAttribute("cabean", cabean);
        }
        return cabean;
    }

}
