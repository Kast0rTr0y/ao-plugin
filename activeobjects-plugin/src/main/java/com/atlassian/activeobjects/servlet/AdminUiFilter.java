package com.atlassian.activeobjects.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.atlassian.activeobjects.servlet.AdminUi.*;
import static com.google.common.base.Preconditions.*;

public final class AdminUiFilter implements Filter
{
    private final AdminUi adminUi;

    public AdminUiFilter(AdminUi adminUi)
    {
        this.adminUi = checkNotNull(adminUi);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse)
        {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, filterChain);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        if (adminUi.isEnabled())
        {
            filterChain.doFilter(request, response);
        }
        else if (isDevModeEnabled())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The Active Objects admin UI is disabled, see the logs for more information.");
        }
        else
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void destroy()
    {
    }
}
