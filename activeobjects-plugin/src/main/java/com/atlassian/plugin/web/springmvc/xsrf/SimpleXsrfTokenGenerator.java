package com.atlassian.plugin.web.springmvc.xsrf;

import com.atlassian.security.random.DefaultSecureTokenGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Cut and paste from atlassian-xwork, just with the session key changed in case the xwork implementation makes
 * incompatible changes in the future. The request parameter name is kept the same as you won't have mixed
 * XWork and SpringMVC forms competing over setting it. (we hope)
 */
public final class SimpleXsrfTokenGenerator implements XsrfTokenGenerator
{
    public static final String TOKEN_SESSION_KEY = "atlassian.xsrf.token";

    public String generateToken(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(TOKEN_SESSION_KEY);

        if (token == null)
        {
            token = createToken();
            session.setAttribute(TOKEN_SESSION_KEY, token);
        }

        return token;
    }

    public String getXsrfTokenName()
    {
        return REQUEST_PARAM_NAME;
    }

    public boolean validateToken(HttpServletRequest request, String token)
    {
        return token != null && token.equals(request.getSession(true).getAttribute(TOKEN_SESSION_KEY));
    }

    private String createToken()
    {
        return DefaultSecureTokenGenerator.getInstance().generateToken();
    }
}
