package com.atlassian.plugin.web.springmvc.xsrf;

import javax.servlet.http.HttpServletRequest;

public interface XsrfTokenGenerator
{
    String REQUEST_PARAM_NAME = "atl_token";

    String generateToken(HttpServletRequest request);

    String getXsrfTokenName();

    boolean validateToken(HttpServletRequest request, String token);
}
