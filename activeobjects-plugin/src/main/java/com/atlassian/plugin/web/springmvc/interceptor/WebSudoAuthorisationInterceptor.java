package com.atlassian.plugin.web.springmvc.interceptor;

import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Requires a WebSudo session to access admin pages.
 */
public final class WebSudoAuthorisationInterceptor extends HandlerInterceptorAdapter
{
    private WebSudoManager webSudoManager;

    public WebSudoAuthorisationInterceptor(WebSudoManager webSudoManager)
    {
        this.webSudoManager = webSudoManager;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        try
        {
            webSudoManager.willExecuteWebSudoRequest(request);
            return true;
        }
        catch (WebSudoSessionException wes)
        {
            webSudoManager.enforceWebSudoProtection(request, response);
            return false;
        }
    }
}
