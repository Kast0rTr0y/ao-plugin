package com.atlassian.activeobjects.junit;

import org.junit.rules.MethodRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

public final class AtlassianPluginsJUnitRunner extends BlockJUnit4ClassRunner
{
    /**
     * @param klass the test class
     * @throws InitializationError if the test class is malformed.
     */
    public AtlassianPluginsJUnitRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    @Override
    protected List<MethodRule> rules(Object test)
    {
        final List<MethodRule> methodRules = new ArrayList<MethodRule>(super.rules(test));
        methodRules.add(0, new AtlassianPluginsMethodRule(test));
        return methodRules;
    }
}
