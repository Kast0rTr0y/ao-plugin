package com.atlassian.plugin.web.springmvc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

public final class DispatcherServlet extends org.springframework.web.servlet.DispatcherServlet implements ApplicationContextAware {
    private ApplicationContext pluginSpringContext;

    public DispatcherServlet() {
        // don't publish the Spring context in the servlet context -- this would affect other plugins
        setPublishContext(false);
    }

    /**
     * In Spring 4 we have to override
     * {@link #initWebApplicationContext()} instead of {@link #findWebApplicationContext()},
     * because initWebApplicationContext has changed from Spring 2.5.6 and now calls
     * {@link org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext(
     * javax.servlet.ServletContext)} in Confluence this returns something referencing confluence's internal copy of
     * spring-web (which got there from
     * {@link org.springframework.web.context.ContextLoader#initWebApplicationContext(javax.servlet.ServletContext)})
     * - that gives a classcast because this plugin is using the plugin framework's spring.
     * <p>
     * This override is safe for our use case with the Spring 4.1.4 implementation, but may be fragile with future upgrades.
     *
     * Possible alternative solution: define a servlet-filter in atlassian-plugin.xml and have it override/hide the
     * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} attribute.
     */
    @Override
    protected WebApplicationContext initWebApplicationContext()
    {
        // use the plugin Spring context as the parent for a new web application context
        XmlWebApplicationContext context = new XmlWebApplicationContext() {

            @Override
            protected void initBeanDefinitionReader(final XmlBeanDefinitionReader beanDefinitionReader)
            {
                beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
                super.initBeanDefinitionReader(beanDefinitionReader);
            }
        };
        context.setId("AOWebApplicationContext");
        context.setParent(pluginSpringContext);
        context.setConfigLocation(getContextConfigLocation());
        context.setServletContext(getServletContext());
        context.refresh();

        // Required because the overridden super method would usually call this
        onRefresh(context);

        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        super.setApplicationContext(applicationContext);
        this.pluginSpringContext = applicationContext;
    }
}
