package com.atlassian.plugin.web.springmvc.interceptor;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The content-type needs to be set so that Sitemesh will decorate responses generated by Velocity in Spring MVC correctly.
 * For some reason, setting the contentType property on the viewResolver doesn't actually work.
 */
public final class ContentTypeInterceptor extends HandlerInterceptorAdapter {
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // TODO: use the application's configured encoding
        response.setContentType("text/html; charset=UTF-8");
    }
}
