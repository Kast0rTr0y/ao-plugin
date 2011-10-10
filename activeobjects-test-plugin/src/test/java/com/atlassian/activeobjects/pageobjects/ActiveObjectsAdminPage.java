package com.atlassian.activeobjects.pageobjects;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;

import java.util.List;

import static com.atlassian.activeobjects.pageobjects.AoTable.*;

public class ActiveObjectsAdminPage implements Page
{
    @ElementBy(tagName = "title")
    PageElement title;

    @ElementBy(tagName = "tbody")
    PageElement tables;

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
        return Lists.transform(tables.findAll(By.tagName("tr")), new Function<PageElement, AoTable>()
        {
            @Override
            public AoTable apply(PageElement row)
            {
                final List<PageElement> cells = row.findAll(By.tagName("td"));
                return table(cells.get(0).getText(), cells.get(1).getText(), cells.get(2).getText());
            }
        });
    }
}
