package com.atlassian.activeobjects.external;

public final class ModelVersion implements Comparable<ModelVersion>
{
    private final int version;

    private ModelVersion(int version)
    {
        this.version = version;
    }

    @Override
    public int compareTo(ModelVersion mv)
    {
        return version - mv.version;
    }

    public boolean isOlderThan(ModelVersion mv)
    {
        return compareTo(mv) < 0;
    }

    public boolean isNewerThan(ModelVersion mv)
    {
        return compareTo(mv) > 0;
    }

    public boolean isSame(ModelVersion mv)
    {
        return compareTo(mv) == 0;
    }

    @Override
    public String toString()
    {
        return String.valueOf(version);
    }

    public static ModelVersion valueOf(String s)
    {
        return s == null ? zero() : new ModelVersion(Integer.valueOf(s));
    }

    private static ModelVersion zero()
    {
        return new ModelVersion(0);
    }
}
