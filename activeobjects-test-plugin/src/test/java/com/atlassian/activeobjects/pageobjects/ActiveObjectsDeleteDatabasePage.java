package com.atlassian.activeobjects.pageobjects;

import com.atlassian.pageobjects.Page;
import com.atlassian.webdriver.AtlassianWebDriver;

import javax.inject.Inject;

public class ActiveObjectsDeleteDatabasePage implements Page
{
    @Inject
    protected AtlassianWebDriver driver;

    @Override
    public String getUrl()
    {
        return "/plugins/servlet/ao-test?delete=true";
    }
}
