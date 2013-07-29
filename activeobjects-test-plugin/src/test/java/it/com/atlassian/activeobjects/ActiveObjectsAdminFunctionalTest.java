package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.pageobjects.ActiveObjectsAdminPage;
import com.atlassian.activeobjects.pageobjects.ActiveObjectsBackupPage;
import com.atlassian.activeobjects.pageobjects.ActiveObjectsDeleteDatabasePage;
import com.atlassian.activeobjects.pageobjects.AoTable;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.Tester;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.List;

import static com.atlassian.activeobjects.pageobjects.AoTable.table;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

public final class ActiveObjectsAdminFunctionalTest
{
    private static final String TEST_PLUGIN_NAME = "ActiveObjects Plugin - Test Plugin";
    private static final Predicate<AoTable> MATCHES_TEST_PLUGIN = new Predicate<AoTable>()
    {
        @Override
        public boolean apply(AoTable table)
        {
            return TEST_PLUGIN_NAME.equals(table.plugin);
        }
    };

    private TestedProduct<? extends Tester> product;

    @Before
    public final void setUp()
    {
        product = TestedProductFactory.create(System.getProperty("tested.app", RefappTestedProduct.class.getName()));
        //delete the database before starting the test
        loginAsSysAdmin(product, ActiveObjectsDeleteDatabasePage.class);
    }

    @Test
    public final void testAdmin()
    {
        final ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        assertThat(admin.getTitle(), containsString("Plugin Data Storage"));

        // this will create the data
        product.visit(ActiveObjectsBackupPage.class);

        assertTables(
                newArrayList(
                        table(TEST_PLUGIN_NAME, "AO_0F732C_LONG_NAME_TO_AUTHOR", "9"),
                        table(TEST_PLUGIN_NAME, "AO_0F732C_AUTHORSHIP", "10"),
                        table(TEST_PLUGIN_NAME, "AO_0F732C_BOOK", "3")),
                Lists.newArrayList(Collections2.filter(product.visit(ActiveObjectsAdminPage.class).getTables(), MATCHES_TEST_PLUGIN)));
    }

    private void assertTables(List<AoTable> expected, List<AoTable> actual)
    {
        assertEquals(expected.size(), actual.size());
        for (AoTable e : expected)
        {
            assertTrue("Didn't find " + e + " in " + actual, actual.contains(e));
        }
    }

    private <P extends Page> P loginAsSysAdmin(TestedProduct<? extends Tester> product, Class<P> nextPage)
    {
        if (!product.visit(HomePage.class).getHeader().isLoggedIn())
        {
            return product.visit(LoginPage.class).loginAsSysAdmin(nextPage);
        }
        else
        {
            return product.visit(nextPage);
        }
    }
}
