package com.atlassian.plugin.web.springmvc.interceptor;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Limits access to users with system administration permission in the application.
 */
public final class SystemAdminAuthorisationInterceptor extends HandlerInterceptorAdapter
{
    private final UserManager userManager;
    private final LoginUriProvider loginUriProvider;
    private final ApplicationProperties applicationProperties;

    public SystemAdminAuthorisationInterceptor(UserManager userManager, LoginUriProvider loginUriProvider, ApplicationProperties applicationProperties)
    {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.applicationProperties = applicationProperties;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        // We require SystemAdmin to prevent normal Administrators being able to elevate their privileges by manipulating
        // the user directories.
        final boolean isSystemAdmin = userManager.isSystemAdmin(userManager.getRemoteUsername(request));
        if (!isSystemAdmin)
        {
            String requestPath = request.getRequestURI().substring(request.getContextPath().length());
            request.getSession().setAttribute("seraph_originalurl", requestPath);
            response.sendRedirect(getRelativeLoginUrl(request.getContextPath(), requestPath));
        }
        return isSystemAdmin;
    }

    /**
     * Strips the application's base URL from the redirect URL so that we redirect to a relative URL.
     * <p/>
     * This handles cases in Confluence clustering tests where we should redirect relative to the current
     * instance rather than to the absolute base URL.
     */
    private String getRelativeLoginUrl(String contextPath, String originalRequestPath) throws URISyntaxException
    {
        String loginUri = loginUriProvider.getLoginUri(new URI(originalRequestPath)).toString();
        if (!loginUri.startsWith(applicationProperties.getBaseUrl()))
        {
            return loginUri;
        }

        loginUri = loginUri.substring(applicationProperties.getBaseUrl().length());
        if (!loginUri.startsWith("/"))
        {
            loginUri = "/" + loginUri;
        }
        return contextPath + loginUri;
    }
}