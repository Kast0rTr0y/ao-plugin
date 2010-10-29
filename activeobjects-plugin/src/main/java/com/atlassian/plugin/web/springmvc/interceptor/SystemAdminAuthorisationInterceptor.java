package com.atlassian.plugin.web.springmvc.interceptor;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Limits access to users with system administration permission in the application.
 */
public final class SystemAdminAuthorisationInterceptor extends HandlerInterceptorAdapter
{
    private UserManager userManager;
    private LoginUriProvider loginUriProvider;

    public SystemAdminAuthorisationInterceptor(UserManager userManager, LoginUriProvider loginUriProvider)
    {
        this.userManager = checkNotNull(userManager);
        this.loginUriProvider = checkNotNull(loginUriProvider);
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        final boolean isSystemAdmin = userManager.isSystemAdmin(userManager.getRemoteUsername(request));
        if (!isSystemAdmin)
        {
            String requestPath = request.getRequestURI().substring(request.getContextPath().length());
            response.sendRedirect(loginUriProvider.getLoginUri(new URI(requestPath)).toString());
        }
        return isSystemAdmin;
    }
}
