package com.atlassian.activeobjects.junit;

import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.*;

public final class AtlassianPluginsMethodRule implements MethodRule
{
    private final Object test;

    private AtlassianPluginsContainer container;
    private Map<Class<?>, Object> mockHostComponents;
    private File tmpDir;

    public AtlassianPluginsMethodRule(Object test)
    {
        this.test = test;
    }

    @Override
    public final Statement apply(final Statement base, final FrameworkMethod method, final Object target)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before();
                try
                {
                    base.evaluate();
                }
                finally
                {
                    after();
                }
            }
        };
    }

    private void before()
    {
        injectHostComponents();
        prepareTmpDir();
        container = new JUnitAtlassianPluginsContainer(new JUnitAtlassianPluginsContainerConfiguration(test.getClass(), mockHostComponents, tmpDir));

        inject(AtlassianPluginsContainer.class, new DelegatingAtlassianPluginsContainer(container)
        {

            @Override
            public void start()
            {
                super.start();
                installPlugins(); // make sure plugins are installed when the container is started
            }
        });
    }

    private void after()
    {
        mockHostComponents.clear();
        cleanTmpDir();
        container = null;
    }

    private void cleanTmpDir()
    {
        delete(tmpDir);
    }

    private void delete(File file)
    {
        if (file == null || !file.exists())
        {
            return;
        }
        if (file.isFile())
        {
            file.delete();
            return;
        }
        else
        {
            for (File f : file.listFiles())
            {
                delete(f);
            }
            file.delete();
        }
    }

    private void prepareTmpDir()
    {
        try
        {
            File file = File.createTempFile("activeobjects", "junit");
            file.delete();
            file.mkdirs();
            tmpDir = file;
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void installPlugins()
    {
        final Plugins plugins = test.getClass().getAnnotation(Plugins.class);
        if (plugins != null)
        {
            for (Class<? extends PluginFile> pluginFileClass : plugins.value())
            {
                final PluginFile pluginFile;
                try
                {
                    pluginFile = pluginFileClass.newInstance();
                }
                catch (InstantiationException e)
                {
                    throw new IllegalStateException(e);
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
                container.install(pluginFile.getPluginFile());
            }
        }
    }

    private <T, I extends T> void inject(Class<T> type, I instance)
    {
        inject(type, instance, test.getClass());
    }

    private <T, I extends T> void inject(Class<T> type, I instance, Class<?> currentClass)
    {
        if (currentClass == Object.class)
        {
            return;
        }

        for (Field field : currentClass.getDeclaredFields())
        {
            if (type.isAssignableFrom(field.getType()))
            {

                final boolean accessible = field.isAccessible();
                try
                {
                    field.setAccessible(true);
                    field.set(test, instance);
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
                finally
                {
                    field.setAccessible(accessible);
                }
            }
        }

        inject(type, instance, currentClass.getSuperclass());
    }

    private void injectHostComponents()
    {
        mockHostComponents = new MapMaker().makeComputingMap(new Function<Class<?>, Object>()
        {
            @Override
            public Object apply(Class<?> from)
            {
                return mock(from);
            }
        });

        injectHostComponents(test.getClass());
    }

    private void injectHostComponents(Class<? extends Object> currentClass)
    {
        if (currentClass.equals(Object.class))
        {
            return;
        }

        for (Field field : currentClass.getDeclaredFields())
        {
            if (field.getAnnotation(MockHostComponent.class) != null && field.getType().isInterface())
            {
                final boolean accessible = field.isAccessible();
                try
                {
                    field.setAccessible(true);
                    field.set(test, mockHostComponents.get(field.getType()));
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
                finally
                {
                    field.setAccessible(accessible);
                }
            }
        }
        injectHostComponents(currentClass.getSuperclass());
    }


    private static final class JUnitAtlassianPluginsContainerConfiguration implements AtlassianPluginsContainerConfiguration
    {
        private final Class<?> testClass;
        private final Map<Class<?>, Object> hostComponents;
        private final File tmpDir;

        public JUnitAtlassianPluginsContainerConfiguration(Class<?> testClass, Map<Class<?>, Object> hostComponents, File tmpDir)
        {
            this.testClass = testClass;
            this.hostComponents = hostComponents;
            this.tmpDir = tmpDir;
        }

        @Override
        public HostComponentProvider getHostComponentProvider()
        {
            return new HostComponentProvider()
            {
                @Override
                public void provide(ComponentRegistrar registrar)
                {
                    for (Map.Entry<Class<?>, Object> hostComponentsEntries : hostComponents.entrySet())
                    {
                        registrar.register(hostComponentsEntries.getKey()).forInstance(hostComponentsEntries.getValue());
                    }
                }
            };
        }

        @Override
        public PackageScannerConfiguration getPackageScannerConfiguration()
        {
            if (testClass.isAnnotationPresent(Host.class))
            {
                final Host host = testClass.getAnnotation(Host.class);
                final DefaultPackageScannerConfiguration scannerConfiguration = new DefaultPackageScannerConfiguration(host.version());
                for (String include : host.includes())
                {
                    scannerConfiguration.getPackageIncludes().add(include);
                }
                for (String exclude : host.excludes())
                {
                    scannerConfiguration.getPackageExcludes().add(exclude);
                }
                for (PackageVersion pv : host.versions())
                {
                    scannerConfiguration.getPackageVersions().put(pv.value(), pv.version());
                }
                return scannerConfiguration;
            }
            else
            {
                return new DefaultPackageScannerConfiguration();
            }
        }

        @Override
        public File getTmpDir()
        {
            return tmpDir;
        }
    }
}
