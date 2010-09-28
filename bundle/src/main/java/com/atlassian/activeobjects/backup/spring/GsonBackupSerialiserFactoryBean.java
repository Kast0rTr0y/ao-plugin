package com.atlassian.activeobjects.backup.spring;

import com.atlassian.activeobjects.backup.GsonBackupSerialiser;
import com.google.gson.reflect.TypeToken;
import net.java.ao.schema.ddl.DDLAction;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Collection;

public final class GsonBackupSerialiserFactoryBean extends AbstractFactoryBean
{
    public Class getObjectType()
    {
        return GsonBackupSerialiser.class;
    }

    protected Object createInstance() throws Exception
    {
        return new GsonBackupSerialiser<Collection<DDLAction>>(new TypeToken<Collection<DDLAction>>()
        {
        }.getType());
    }
}
