package org.ejbca.core.ejb.vpn;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.AbstractTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Created by dusanklinec on 16.01.17.
 */
public class TemplateHelper {

    /**
     * Default resolver settings
     * @param resolver
     */
    public static void setupResolver(AbstractConfigurableTemplateResolver resolver){
        resolver.setCacheable(true);
        resolver.setCacheTTLMs(5000L);
        resolver.setTemplateMode(TemplateMode.HTML);
    }

    public static TemplateEngine getTemplateEngine(String lang){

        final TemplateEngine templateEngine = new TemplateEngine();
        int order = 1;

        // Language based resolver - if language was specified.
        // Each user we send an email to should have a language specified.
        // Then we send him localized email.
        if (lang != null) {
            final FileTemplateResolver langResolver = new FileTemplateResolver();
            setupResolver(langResolver);
            langResolver.setName("LanguageTemplates");
            langResolver.setSuffix(String.format("-%s.html", VpnUtils.sanitizeFileName(lang)));
            langResolver.setOrder(order);
            templateEngine.addTemplateResolver(langResolver);
            order += 1;
        }

        final FileTemplateResolver tplResolver = new FileTemplateResolver();
        setupResolver(tplResolver);
        tplResolver.setName("LanguageTemplates");
        tplResolver.setSuffix(".html");
        tplResolver.setOrder(order);
        templateEngine.addTemplateResolver(tplResolver);
        order += 1;

        // ...
        return templateEngine;
    }
}
