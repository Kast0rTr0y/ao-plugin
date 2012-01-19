package com.atlassian.activeobjects.pageobjects;




import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;

import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;

import static com.atlassian.activeobjects.pageobjects.AoTable.*;

public class ActiveObjectsAdminPage implements Page
{
    @Override
    public String getUrl()
    {
        return "/plugins/servlet/active-objects/tables/list";
    }

    @Inject
    PageBinder binder;

    @ElementBy(tagName = "title")
    PageElement title;

    @ElementBy(tagName = "tbody")
    PageElement plugins;

    @ElementBy(className = "delete-action")
    PageElement deleteButton;

    @ElementBy(className = "aui-message")
    PageElement message;

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

    public Dialog deleteTables(List<AoTable> tables)
    {
        List<String> tablesToDelete = Lists.transform(tables, new Function<AoTable, String>() {
            @Override
            public String apply(AoTable from)
            {
                return from.table;
            }
        });

        for (PageElement checkbox : plugins.findAll(By.tagName("input")))
        {
            if (checkbox.hasAttribute("type", "checkbox") && checkbox.hasAttribute("name", "tableNames"))
            {
                String table = checkbox.getAttribute("value");
                if (tablesToDelete.contains(table) != checkbox.isSelected())
                {
                    checkbox.toggle();
                }
            }
        }

        deleteButton.click();
        return binder.bind(Dialog.class);
    }

    public boolean hasSuccessMessage()
    {
        return message.isPresent() && message.hasClass("success");
    }

    public boolean hasErrorMessage()
    {
        return message.isPresent() && message.hasClass("error");
    }

    public static class Dialog
    {
        @ElementBy(className = "confirm-button")
        PageElement confirmButton;

        @ElementBy(className = "close-button")
        PageElement closeButton;

        @Inject
        PageBinder binder;

        public boolean canConfirm()
        {
            return confirmButton.isPresent();
        }

        public ActiveObjectsAdminPage cancel()
        {
            closeButton.click();
            return binder.bind(ActiveObjectsAdminPage.class);
        }

        public ActiveObjectsAdminPage confirm()
        {
            confirmButton.click();
            return binder.bind(ActiveObjectsAdminPage.class);
        }
    }
}
