package com.atlassian.activeobjects.backup;

import org.osgi.framework.Bundle;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A backup ID
 */
final class BackupId
{
    private static final String SEPARATOR = "%";
    private static final String PREFIX = "ao";
    private final String name;
    private final String version;

    private BackupId(String name, String version)
    {
        this.name = escape(name);
        this.version = escape(version);
    }

    static BackupId fromBundle(Bundle bundle)
    {
        return new BackupId(bundle.getSymbolicName(), bundle.getVersion().toString());
    }

    static BackupId fromString(String id)
    {
        final StringTokenizer st = new StringTokenizer(id, SEPARATOR);
        st.nextToken(); // removing the prefix
        return new BackupId(st.nextToken(), st.nextToken());
    }

    public boolean isCompatible(BackupId backupId)
    {
        return backupId != null && backupId.name.equals(this.name);
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
                .append(PREFIX)
                .append(SEPARATOR)
                .append(name)
                .append(SEPARATOR)
                .append(version)
                .toString();
    }

    private String escape(String s)
    {
        return s.replaceAll(Pattern.quote(SEPARATOR), "_");
    }
}
