package com.atlassian.activeobjects.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link DelegatingActiveObjectsFactoryResolver}
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingActiveObjectsFactoryResolverTest
{
    private ActiveObjectsFactoryResolver resolver;

    @Mock
    private ActiveObjectsFactoryResolver actualResolver;

    @Before
    public void setUp() throws Exception
    {
        resolver = new DelegatingActiveObjectsFactoryResolver(Collections.singletonList(actualResolver));
    }

    @After
    public void tearDown() throws Exception
    {
        resolver = null;
        actualResolver = null;
    }

    @Test
    public void testAcceptReturnsFalseWhenDelegatesReturnFalse() throws Exception
    {
        final DataSourceType dataSourceType = getDataSourceType();

        when(actualResolver.accept(dataSourceType)).thenReturn(false);
        assertFalse(resolver.accept(dataSourceType));
    }

    @Test
    public void testAcceptReturnsTrueWhenDelegatesReturnTrue() throws Exception
    {
        final DataSourceType dataSourceType = getDataSourceType();

        when(actualResolver.accept(dataSourceType)).thenReturn(true);
        assertTrue(resolver.accept(dataSourceType));
    }

    @Test
    public void testGetReturnsDelegateValueWhenDelgatesAcceptReturnsTrue() throws Exception
    {
        final DataSourceType dataSourceType = getDataSourceType();
        final ActiveObjectsFactory activeObjectsFactory = mock(ActiveObjectsFactory.class);

        when(actualResolver.accept(dataSourceType)).thenReturn(true);
        when(actualResolver.get(dataSourceType)).thenReturn(activeObjectsFactory);

        assertEquals(activeObjectsFactory, resolver.get(dataSourceType));
    }

    @Test
    public void testGetThrowsExceptionWhenNoDelgatesAcceptReturnsTrue() throws Exception
    {
        final DataSourceType dataSourceType = getDataSourceType();
        try
        {
            when(actualResolver.accept(dataSourceType)).thenReturn(false);

            resolver.get(dataSourceType);
            fail("Should have thrown " + CannotResolveActiveObjectsFactoryException.class.getName());
        }
        catch (CannotResolveActiveObjectsFactoryException e)
        {
            assertEquals(dataSourceType, e.getDataSourceType());
        }
    }

    private DataSourceType getDataSourceType()
    {
        return DataSourceType.APPLICATION;
    }
}
