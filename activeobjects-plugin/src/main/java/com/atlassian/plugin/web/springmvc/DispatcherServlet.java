package com.atlassian.plugin.web.springmvc;

import org.springframework.beans.BeansException;
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
        XmlWebApplicationContext context = new XmlWebApplicationContext();
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
