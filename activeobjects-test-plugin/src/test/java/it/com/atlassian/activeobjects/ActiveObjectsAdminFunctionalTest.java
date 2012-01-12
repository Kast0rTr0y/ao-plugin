package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.pageobjects.ActiveObjectsAdminPage;
import com.atlassian.activeobjects.pageobjects.ActiveObjectsBackupPage;
import com.atlassian.activeobjects.pageobjects.AoTable;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.Tester;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.refapp.RefappTestedProduct;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.atlassian.activeobjects.pageobjects.AoTable.*;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;

public final class ActiveObjectsAdminFunctionalTest
{
    private static final String TEST_PLUGIN_NAME = "ActiveObjects Plugin - Test Plugin";

    private TestedProduct<? extends Tester> product;

    @Before
    public final void setUp()
    {
        product = TestedProductFactory.create(System.getProperty("tested.app", RefappTestedProduct.class.getName()));
    }

    @Test
    public final void testAdmin()
    {
        final ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        assertTrue(admin.getTitle().contains("Plugin Data Storage"));

        assertTables(Lists.<AoTable>newArrayList(), admin.getTables());

        // this will create the tables
        product.visit(ActiveObjectsBackupPage.class);

        assertTables(
                newArrayList(
                        table(TEST_PLUGIN_NAME, "AO_0F732C_LONG_NAME_TO_AUTHOR", "9"),
                        table(TEST_PLUGIN_NAME, "AO_0F732C_AUTHORSHIP", "10"),
                        table(TEST_PLUGIN_NAME, "AO_0F732C_BOOK", "3")),
                product.visit(ActiveObjectsAdminPage.class).getTables());
    }

    private void assertTables(List<AoTable> expected, List<AoTable> actual)
    {
        assertEquals(expected.size(), actual.size());
        for (AoTable e : expected)
        {
            assertTrue("Didn't find " + e + " in " + actual, actual.contains(e));
        }
    }


    /**
     * Deleting all tables should always be successful undepending of FK, because tables are sorted.
     */
    @Test
    public final void testDeleteAllTables()
    {
        // this will create the tables
        product.visit(ActiveObjectsBackupPage.class);

        ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        ActiveObjectsAdminPage.Dialog confirmDeletion = admin.deleteTables(admin.getTables());
        assertTrue("The user should be proposed to confirm the deletion", confirmDeletion.canConfirm());
        admin = confirmDeletion.confirm();

        assertTrue("A success message should be displayed", admin.hasSuccessMessage());
        assertEquals("All tables should be deleted", 0, admin.getTables().size());
    }

    /**
     * Deleting all tables should always be successful undepending of FKs, because tables are sorted.
     */
    @Test
    public final void testCancelDeletion()
    {
        // this will create the tables
        product.visit(ActiveObjectsBackupPage.class);

        ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        ActiveObjectsAdminPage.Dialog confirmDeletion = admin.deleteTables(admin.getTables());
        assertTrue("The user should be proposed to confirm the deletion", confirmDeletion.canConfirm());
        admin = confirmDeletion.cancel();

        assertFalse("No action should have been taken.", admin.hasSuccessMessage());
        assertFalse("No action should have been taken.", admin.hasErrorMessage());
        assertEquals("No table should be deleted", 3, admin.getTables().size());
    }

    /**
     * Deleting one table which is the target of a FK shouldn't be successful.
     */
    @Test
    public final void testDeleteTableWithForeignKeyReturnsErrorMessage()
    {
        // this will create the tables
        product.visit(ActiveObjectsBackupPage.class);

        ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        AoTable authorTable = Iterables.find(admin.getTables(), new Predicate<AoTable>()
        {
            @Override
            public boolean apply(AoTable input)
            {
                return "AO_0F732C_LONG_NAME_TO_AUTHOR".equals(input.table);
            }
        });

        ActiveObjectsAdminPage.Dialog confirmDeletion = admin.deleteTables(Lists.<AoTable>newArrayList(authorTable));
        assertTrue("The user should be proposed to confirm the deletion", confirmDeletion.canConfirm());
        admin = confirmDeletion.confirm();

        assertTrue("It should not be possible to drop a table which is the target of a foreign key", admin.hasErrorMessage());
        assertEquals("No table should be deleted", 3, admin.getTables().size());
    }

    /**
     * Deleting all tables should always be successful undepending of FK, because tables are sorted.
     */
    @Test
    public final void testDeleteNoTableDisplaysErrorMessage()
    {
        // this will create the tables
        product.visit(ActiveObjectsBackupPage.class);

        ActiveObjectsAdminPage admin = loginAsSysAdmin(product, ActiveObjectsAdminPage.class);
        ActiveObjectsAdminPage.Dialog confirmDeletion = admin.deleteTables(Collections.<AoTable>emptyList());
        assertFalse("No table was selected for deletion. The action should not be possible.", confirmDeletion.canConfirm());
        confirmDeletion.cancel();
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
