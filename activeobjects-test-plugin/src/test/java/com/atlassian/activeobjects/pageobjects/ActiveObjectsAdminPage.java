package com.atlassian.activeobjects.pageobjects;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;

import java.util.LinkedList;
import java.util.List;

import static com.atlassian.activeobjects.pageobjects.AoTable.*;

public class ActiveObjectsAdminPage implements Page
{
    @ElementBy(tagName = "title")
    PageElement title;

    @ElementBy(tagName = "tbody")
    PageElement plugins;

    @Override
    public String getUrl()
    {
        return "/plugins/servlet/active-objects/tables/list";
    }

    public String getTitle()
    {
        return title.getText();
    }

    public List<AoTable> getTables()
    {
        final List<AoTable> tables = new LinkedList<AoTable>();
        for (PageElement tr : plugins.findAll(By.tagName("tr")))
        {
            final String pluginName = tr.find(By.className("ao-plugin-name")).getText();
            final List<PageElement> tableNames = tr.find(By.className("ao-table-names")).findAll(By.tagName("li"));
            final List<PageElement> rowCounts = tr.find(By.className("ao-row-counts")).findAll(By.tagName("li"));

            for (int i = 0; i < tableNames.size(); i++)
            {
                tables.add(table(pluginName, tableNames.get(i).getText(), rowCounts.get(i).getText()));
            }
        }
        return tables;
    }
}
