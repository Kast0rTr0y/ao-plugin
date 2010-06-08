package com.atlassian.activeobjects.internal;

import net.java.ao.RawEntity;
import net.java.ao.schema.AbstractTableNameConverter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Locale;

public class PluginTableNameConverter extends AbstractTableNameConverter {

    protected String tableNamePrefix;

    public PluginTableNameConverter(String pluginKey) {
        generateTableNamePrefix(pluginKey);
    }

    protected void generateTableNamePrefix(String pluginKey) {
        this.tableNamePrefix = "ao_"+ DigestUtils.shaHex(pluginKey).substring(0, 5) +"_";
    }

    @Override
    protected String convertName(Class<? extends RawEntity<?>> entity) {
        String name = tableNamePrefix + entity.getSimpleName().toLowerCase(Locale.ENGLISH);

        if (name.length() > 30)
        {
            int maxLength = 30 - tableNamePrefix.length();
            throw new EntityNameTooLongException("Your entity name ia longer than "+ maxLength +" chars.");
        }

        return name;
    }

}
