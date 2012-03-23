package com.atlassian.plugin.web.springmvc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

public final class DispatcherServlet extends org.springframework.web.servlet.DispatcherServlet implements ApplicationContextAware
{
    private ApplicationContext pluginSpringContext;

    public DispatcherServlet()
    {
        // don't publish the Spring context in the servlet context -- this would affect other plugins
        setPublishContext(false);
    }

    @Override
    protected WebApplicationContext findWebApplicationContext()
    {
        // use the plugin Spring context as the parent for a new web application context
        final XmlWebApplicationContext context = new XmlWebApplicationContext()
        {
            @Override
            protected void initBeanDefinitionReader(final XmlBeanDefinitionReader beanDefinitionReader)
            {
                beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
                super.initBeanDefinitionReader(beanDefinitionReader);
            }
        };
        context.setId("ECWebApplicationContext");
        context.setParent(pluginSpringContext);
        context.setConfigLocation(getContextConfigLocation());
        context.setServletContext(getServletContext());
        context.refresh();
        return context;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.pluginSpringContext = applicationContext;
    }
}
