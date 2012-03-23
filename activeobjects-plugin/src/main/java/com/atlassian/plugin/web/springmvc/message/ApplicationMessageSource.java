package com.atlassian.plugin.web.springmvc.message;

import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.context.support.AbstractMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Looks up Spring MVC message codes using the application's {@link com.atlassian.sal.api.message.I18nResolver} and a customisable key prefix.
 * The prefix defaults to an empty string ("").
 */
public final class ApplicationMessageSource extends AbstractMessageSource
{
    private String keyPrefix = "";
    private I18nResolver i18nResolver;

    protected MessageFormat resolveCode(String code, Locale locale)
    {
        return new MessageFormat(i18nResolver.getText(keyPrefix + code));
    }

    protected String resolveCodeWithoutArguments(String code, Locale locale)
    {
        return i18nResolver.getText(keyPrefix + code);
    }

    public void setKeyPrefix(String propertyPrefix)
    {
        this.keyPrefix = propertyPrefix;
    }

    public void setI18nResolver(I18nResolver i18nResolver)
    {
        this.i18nResolver = i18nResolver;
    }
}
