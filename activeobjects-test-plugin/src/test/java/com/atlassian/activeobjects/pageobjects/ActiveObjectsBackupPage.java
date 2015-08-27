package com.atlassian.activeobjects.pageobjects;

import com.atlassian.pageobjects.Page;
import com.atlassian.webdriver.AtlassianWebDriver;

import javax.inject.Inject;

public final class ActiveObjectsBackupPage implements Page {
    @Inject
    protected AtlassianWebDriver driver;

    @Override
    public String getUrl() {
        return "/plugins/servlet/ao-test?create=true";
    }
}
