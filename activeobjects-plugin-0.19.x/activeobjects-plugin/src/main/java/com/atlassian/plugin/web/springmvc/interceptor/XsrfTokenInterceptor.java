package com.atlassian.plugin.web.springmvc.interceptor;

import com.atlassian.plugin.web.springmvc.xsrf.XsrfTokenGenerator;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Small, self-contained implementation of the XSRF token generator, copied directly from our XWork implementation.
 * Due to Spring MVC being somewhat simplistic (or less generously, pants-on-head stupid) we can't hook into validation
 * the way the XWork implementation does, so we settle ourselves with just redirecting to the plugin's front page.
 *
 * <p>The interceptor also ensures that the token value is available in the model under the key $xsrfTokenValue
 */
public final class XsrfTokenInterceptor extends HandlerInterceptorAdapter
{
    private ApplicationProperties applicationProperties;
    private XsrfTokenGenerator xsrfTokenGenerator;
    private String redirectPath = "";

    public XsrfTokenInterceptor()
    {
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String token = request.getParameter(XsrfTokenGenerator.REQUEST_PARAM_NAME);
        if ("POST".equals(request.getMethod()) && !xsrfTokenGenerator.validateToken(request, token))
        {
            response.sendRedirect(applicationProperties.getBaseUrl() + request.getServletPath() + redirectPath);
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> model = modelAndView.getModel();
        model.put("xsrfTokenName", xsrfTokenGenerator.getXsrfTokenName());
        model.put("xsrfTokenValue", xsrfTokenGenerator.generateToken(request));
    }

    public void setApplicationProperties(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    public void setRedirectPath(String redirectPath)
    {
        this.redirectPath = redirectPath;
    }

    public void setXsrfTokenGenerator(XsrfTokenGenerator xsrfTokenGenerator)
    {
        this.xsrfTokenGenerator = xsrfTokenGenerator;
    }
}
