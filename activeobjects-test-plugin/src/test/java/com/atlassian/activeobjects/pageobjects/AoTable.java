package com.atlassian.activeobjects.pageobjects;

public final class AoTable {
    public final String plugin;
    public final String table;
    public final String rows;

    private AoTable(String plugin, String table, String rows) {
        this.plugin = plugin;
        this.table = table;
        this.rows = rows;
    }

    public static AoTable table(String plugin, String table, String rows) {
        return new AoTable(plugin, table, rows);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AoTable aoTable = (AoTable) o;

        if (plugin != null ? !plugin.equals(aoTable.plugin) : aoTable.plugin != null) {
            return false;
        }
        if (rows != null ? !rows.equals(aoTable.rows) : aoTable.rows != null) {
            return false;
        }
        if (table != null ? !table.equals(aoTable.table) : aoTable.table != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = plugin != null ? plugin.hashCode() : 0;
        result = 31 * result + (table != null ? table.hashCode() : 0);
        result = 31 * result + (rows != null ? rows.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AoTable{" +
                "plugin='" + plugin + '\'' +
                ", table='" + table + '\'' +
                ", rows='" + rows + '\'' +
                '}';
    }
}
