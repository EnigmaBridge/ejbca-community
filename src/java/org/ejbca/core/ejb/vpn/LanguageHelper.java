package org.ejbca.core.ejb.vpn;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalEjbcaResources;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Language related tools.
 * We need language settings also in the lower layers of th EJBCA - e.g., in the CLI.
 * Mainly for sending localised emails to the users.
 *
 * Created by dusanklinec on 16.01.17.
 */
public class LanguageHelper {
    private static final Logger log = Logger.getLogger(LanguageHelper.class);

    /** Internal localization of logs and errors */
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    /**
     * Returns locale for the language tag.
     * @param language language tag (e.g., cs, en)
     * @return Locale corresponding to the language tag
     */
    public static Locale getLocale(String language){
        return language == null ? null : Locale.forLanguageTag(language);
    }

    /**
     * Loads default language resource file.
     * @return ResourceBundle
     * @throws IOException in case of file manipulation
     */
    public static ResourceBundle loadLanguageResource() throws IOException {
        return loadLanguageResource((Locale) null);
    }

    /**
     * Loads language resource file according to the locale.
     * @param language if null, EN is used.
     * @return ResourceBundle
     * @throws IOException in case of file manipulation
     */
    public static ResourceBundle loadLanguageResource(String language) throws IOException {
        return loadLanguageResource(getLocale(language));
    }

    /**
     * Loads language resource file according to the locale.
     * @param locale if null, EN is used.
     * @return ResourceBundle
     * @throws IOException in case of file manipulation
     */
    public static ResourceBundle loadLanguageResource(Locale locale) throws IOException {
        final File languageDir = VpnConfig.getLanguageDir();
        final URL[] urls = {
                languageDir.toURI().toURL()
        };

        final ClassLoader loader = new URLClassLoader(urls);
        final ResourceBundle rb = ResourceBundle.getBundle(
                VpnCons.VPN_LANGUAGE_FILE, locale == null ? getLocale("en") : locale, loader);
        return rb;
    }

    /**
     * Initializes basic template engine.
     * @return initialized template engine.
     */
    public static TemplateEngine getTemplateEngine() throws IOException {
        return getTemplateEngine(null);
    }

    /**
     * Default resolver settings
     * @param resolver Thymeleaf template resolver
     */
    public static void setupResolver(AbstractConfigurableTemplateResolver resolver){
        resolver.setCacheable(true);
        resolver.setCacheTTLMs(60000L); // 1 minute caching
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.getHtmlTemplateModePatternSpec().addPattern(".*\\.html");
        resolver.getTextTemplateModePatternSpec().addPattern(".*\\.txt");
        resolver.getTextTemplateModePatternSpec().addPattern(".*\\.ovpn");
        resolver.getTextTemplateModePatternSpec().addPattern(".*\\.pem");
        resolver.setCheckExistence(true);
    }

    /**
     * Initializes basic template engine.
     * @param lang nullable preferred language
     * @return initialized template engine.
     */
    public static TemplateEngine getTemplateEngine(String lang) throws IOException {
        final TemplateEngine templateEngine = new TemplateEngine();
        int order = 1;

        final String templatePrefix = VpnConfig.getTemplateDir().getCanonicalPath() + "/";

        // Language based resolver - if language was specified.
        // Each user we send an email to should have a language specified.
        // Then we send him localized email.
        // This could potentially break IMessageResolver as it looks for email-en-en.properties
        if (lang != null && !lang.isEmpty()) {
            final FileTemplateResolver langResolver = new FileTemplateResolver();
            setupResolver(langResolver);
            langResolver.setName("LanguageTemplates");
            langResolver.setPrefix(templatePrefix);
            langResolver.setSuffix(String.format("-%s.html", VpnUtils.sanitizeFileName(lang)));
            langResolver.setOrder(order);
            templateEngine.addTemplateResolver(langResolver);
            order += 1;
        }

        // Default thymeleaf template resolver.
        // IMessageResolver is looking for the properties file named in the same way as the template
        // So this should be enough.
        final FileTemplateResolver tplResolver = new FileTemplateResolver();
        setupResolver(tplResolver);
        tplResolver.setName("templates");
        tplResolver.setPrefix(templatePrefix);
        tplResolver.setSuffix(".html");
        tplResolver.setOrder(order);
        templateEngine.addTemplateResolver(tplResolver);
        order += 1;

        // OVPN resolver
        final FileTemplateResolver vpnResolver = new FileTemplateResolver();
        setupResolver(vpnResolver);
        vpnResolver.setTemplateMode(TemplateMode.TEXT);
        vpnResolver.setName("OVPN");
        vpnResolver.setPrefix(templatePrefix);
        vpnResolver.setSuffix(".ovpn");
        vpnResolver.setOrder(order);
        templateEngine.addTemplateResolver(vpnResolver);
        order += 1;

        // OVPN resolver
        final FileTemplateResolver txtResolver = new FileTemplateResolver();
        setupResolver(txtResolver);
        txtResolver.setTemplateMode(TemplateMode.TEXT);
        txtResolver.setName("Text");
        txtResolver.setPrefix(templatePrefix);
        txtResolver.setSuffix(".txt");
        txtResolver.setOrder(order);
        templateEngine.addTemplateResolver(txtResolver);
        order += 1;

        // ...
        return templateEngine;
    }
}
